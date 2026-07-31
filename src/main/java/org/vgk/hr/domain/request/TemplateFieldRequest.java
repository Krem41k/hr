package org.vgk.hr.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.vgk.hr.db.entity.TemplateFieldType;

@Schema(description = "Позиция и оформление текста в PDF")
public record TemplateFieldRequest(
        @Schema(description = "Значение для подстановки", example = "FULL_NAME")
        TemplateFieldType type,
        @Schema(description = "Номер страницы PDF, начиная с 1", example = "1")
        int pageNumber,
        @Schema(description = "Координата левого нижнего угла по оси X", example = "150")
        double x,
        @Schema(description = "Координата левого нижнего угла по оси Y", example = "400")
        double y,
        @Schema(description = "Ширина области замены текста", example = "200")
        double width,
        @Schema(description = "Высота области замены текста", example = "30")
        double height,
        @Schema(description = "Размер шрифта в пунктах", example = "12")
        double fontSize,
        @Schema(description = "Имя шрифта", example = "DEJAVU_SANS")
        String fontName,
        @Schema(description = "Цвет текста в формате HEX", example = "#000000")
        String color,
        @Schema(description = "Жирное начертание шрифта", example = "false")
        Boolean bold,
        @Schema(
                description = "Шаблон даты для CURRENT_DATE и BIRTH_DATE",
                example = "dd.MM.yyyy"
        )
        String dateFormat
) {
}
