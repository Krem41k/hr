package org.vgk.hr.position;

import io.swagger.v3.oas.annotations.media.Schema;
import org.vgk.hr.db.entity.FieldValueType;

@Schema(description = "Поле динамической формы оператора")
public record FormFieldSchema(
        @Schema(description = "Код поля", example = "fullName") String fieldCode,
        @Schema(description = "Подпись", example = "ФИО") String label,
        @Schema(description = "Тип значения", example = "TEXT") FieldValueType valueType
) {
}
