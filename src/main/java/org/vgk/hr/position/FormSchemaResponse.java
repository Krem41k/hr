package org.vgk.hr.position;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Схема формы для должности: уникальные USER-поля из активных PDF")
public record FormSchemaResponse(
        @Schema(description = "Идентификатор должности") Long positionId,
        @Schema(description = "Поля для заполнения оператором") List<FormFieldSchema> fields
) {
}
