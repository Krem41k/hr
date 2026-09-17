package org.vgk.hr.generation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/positions/{positionId}")
@RequiredArgsConstructor
@Tag(name = "Generation", description = "Генерация пакета документов должности")
public class DocumentPackController {

    /** Base64(UTF-8) темы письма: HTTP-заголовки не переносят кириллицу как есть. */
    public static final String EMAIL_SUBJECT_HEADER = "X-Email-Subject";
    /** Base64(UTF-8) тела письма. */
    public static final String EMAIL_BODY_HEADER = "X-Email-Body";
    /** Кодировка значений email-заголовков; фиксирована как base64. */
    public static final String EMAIL_ENCODING_HEADER = "X-Email-Encoding";
    public static final String EMAIL_ENCODING = "base64";

    private final DocumentPackService documentPackService;

    @Operation(
            summary = "Сгенерировать пакет документов",
            description = """
                    Заполняет все активные PDF-шаблоны должности и возвращает ZIP.
                    Тема и тело письма — в заголовках X-Email-Subject и X-Email-Body \
                    как Base64 от UTF-8 (X-Email-Encoding: base64).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "ZIP с заполненными PDF",
                    content = @Content(mediaType = "application/zip")
            ),
            @ApiResponse(responseCode = "400", description = "Не переданы или некорректны значения полей", content = @Content),
            @ApiResponse(responseCode = "404", description = "Должность не найдена", content = @Content),
            @ApiResponse(responseCode = "409", description = "Должность неактивна или без активных шаблонов", content = @Content)
    })
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(
            @PathVariable Long positionId,
            @RequestBody GenerateDocumentPackRequest request
    ) throws IOException {
        GeneratedDocumentPack pack = documentPackService.generate(positionId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(pack.fileName(), StandardCharsets.UTF_8)
                .build());
        headers.set(EMAIL_ENCODING_HEADER, EMAIL_ENCODING);
        headers.set(EMAIL_SUBJECT_HEADER, encode(pack.emailSubject()));
        headers.set(EMAIL_BODY_HEADER, encode(pack.emailBody()));

        return ResponseEntity.ok().headers(headers).body(pack.zip());
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
