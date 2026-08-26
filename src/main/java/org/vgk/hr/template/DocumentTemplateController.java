package org.vgk.hr.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/positions/{positionId}/templates")
@RequiredArgsConstructor
@Tag(name = "Document templates", description = "PDF-шаблоны должности")
public class DocumentTemplateController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DocumentTemplateService documentTemplateService;

    @Operation(summary = "Список активных PDF-шаблонов должности")
    @GetMapping
    public List<DocumentTemplateResponse> list(@PathVariable Long positionId) {
        return documentTemplateService.list(positionId);
    }

    @Operation(summary = "Получить PDF-шаблон")
    @GetMapping("/{templateId}")
    public DocumentTemplateResponse get(@PathVariable Long positionId, @PathVariable Long templateId) {
        return documentTemplateService.get(positionId, templateId);
    }

    @Operation(summary = "Загрузить PDF-шаблон в пакет должности")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentTemplateResponse> create(
            @PathVariable Long positionId,
            @Parameter(description = "PDF-файл", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(
                    description = "JSON: name, sortOrder, fields",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DocumentTemplateUploadRequest.class)
                    )
            )
            @RequestPart("metadata") byte[] metadataJson
    ) throws IOException {
        DocumentTemplateUploadRequest metadata = OBJECT_MAPPER.readValue(metadataJson, DocumentTemplateUploadRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentTemplateService.create(positionId, file, metadata));
    }

    @Operation(summary = "Обновить PDF-шаблон (soft-replace полей)")
    @PutMapping(value = "/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentTemplateResponse update(
            @PathVariable Long positionId,
            @PathVariable Long templateId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") byte[] metadataJson
    ) throws IOException {
        DocumentTemplateUploadRequest metadata = OBJECT_MAPPER.readValue(metadataJson, DocumentTemplateUploadRequest.class);
        return documentTemplateService.update(positionId, templateId, file, metadata);
    }

    @Operation(summary = "Мягко удалить PDF-шаблон")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(@PathVariable Long positionId, @PathVariable Long templateId) {
        documentTemplateService.softDelete(positionId, templateId);
        return ResponseEntity.noContent().build();
    }
}
