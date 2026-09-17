package org.vgk.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.domain.request.TextEditRequest;
import org.vgk.hr.domain.request.TemplateUploadRequest;
import org.vgk.hr.service.PdfEditorService;
import org.vgk.hr.service.PdfTemplateService;
import org.vgk.hr.shared.TextEditFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File (legacy)", description = "Устаревший API одиночного шаблона; используйте /api/v1/positions")
public class FileController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PdfEditorService pdfEditorService;
    private final PdfTemplateService pdfTemplateService;

    @Operation(
            summary = "Сохранить или обновить PDF-шаблон (устарело)",
            description = "Используйте POST /api/v1/positions/{positionId}/templates.",
            deprecated = true
    )
    @ApiResponse(responseCode = "201", description = "Шаблон сохранён")
    @Deprecated(forRemoval = true)
    @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadTemplate(
            @Parameter(description = "PDF-шаблон", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(
                    description = "JSON с номером шаблона и координатами полей",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TemplateUploadRequest.class)
                    )
            )
            @RequestPart("metadata") byte[] metadataJson) throws IOException {

        TemplateUploadRequest metadata = OBJECT_MAPPER.readValue(metadataJson, TemplateUploadRequest.class);
        log.info(
                "Template upload received: templateNumber={}, fileName={}, fileSize={} bytes, fields={}",
                metadata.templateNumber(),
                file.getOriginalFilename(),
                file.getSize(),
                metadata.fields() == null ? 0 : metadata.fields().size()
        );
        pdfTemplateService.saveTemplate(file, metadata);
        log.info("Template upload completed: templateNumber={}, status=201", metadata.templateNumber());
        return ResponseEntity.status(201).build();
    }

    @Operation(
            summary = "Сгенерировать PDF (устарело)",
            description = "Используйте POST /api/v1/positions/{positionId}/generate — он возвращает ZIP и текст письма.",
            deprecated = true
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сгенерированный PDF-файл",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)
    )
    @ApiResponse(responseCode = "500", description = "Ошибка генерации PDF")
    @Deprecated(forRemoval = true)
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateInvoice(
            @Parameter(description = "Номер сохранённого шаблона", example = "1", required = true)
            @RequestParam("templateNumber") Integer templateNumber,
            @Parameter(description = "Фамилия, имя и отчество", example = "Иванов Иван Иванович", required = true)
            @RequestParam("fullName") String fullName,
            @Parameter(description = "Дата рождения в формате ISO (yyyy-MM-dd)", example = "1990-05-15", required = true)
            @RequestParam("birthDate") LocalDate birthDate) throws IOException {

        log.info(
                "PDF generation received: templateNumber={}, fullName={}, birthDate={}",
                templateNumber,
                fullName,
                birthDate
        );
        PdfTemplate template = pdfTemplateService.getTemplate(templateNumber);
        List<TextEditRequest> edits = buildEdits(template, fullName, birthDate);
        byte[] pdfBytes = pdfEditorService.editPdf(template.getContent(), edits);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "template-%d.pdf".formatted(templateNumber));

        log.info(
                "PDF generation completed: templateNumber={}, responseSize={} bytes, status=200",
                templateNumber,
                pdfBytes.length
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private List<TextEditRequest> buildEdits(PdfTemplate template, String fullName, LocalDate birthDate) {
        return template.getActiveFields().stream()
                .map(field -> TextEditFactory.from(field, valueFor(field, fullName, birthDate)))
                .toList();
    }

    private String valueFor(TemplateField field, String fullName, LocalDate birthDate) {
        String code = WellKnownFieldCodes.normalize(field.getFieldCode());
        return switch (code) {
            case WellKnownFieldCodes.FULL_NAME -> fullName;
            case WellKnownFieldCodes.BIRTH_DATE -> formatDate(birthDate, field.getDateFormat());
            case WellKnownFieldCodes.CURRENT_DATE -> formatDate(LocalDate.now(), field.getDateFormat());
            default -> throw new IllegalArgumentException(
                    "Unsupported fieldCode for generate API: " + field.getFieldCode()
            );
        };
    }

    private String formatDate(LocalDate date, String dateFormat) {
        DateTimeFormatter formatter = dateFormat == null || dateFormat.isBlank()
                ? DateTimeFormatter.ISO_LOCAL_DATE
                : DateTimeFormatter.ofPattern(dateFormat, Locale.forLanguageTag("ru"));
        return date.format(formatter);
    }
}
