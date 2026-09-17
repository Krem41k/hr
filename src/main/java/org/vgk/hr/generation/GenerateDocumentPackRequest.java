package org.vgk.hr.generation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Значения полей для генерации пакета документов")
public record GenerateDocumentPackRequest(
        @Schema(
                description = "Значения по fieldCode из form-schema; DATE-поля в формате ISO (yyyy-MM-dd)",
                example = "{\"fullName\": \"Иванов Иван Иванович\", \"birthDate\": \"1990-05-15\"}"
        )
        Map<String, String> fields
) {
}
