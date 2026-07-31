package org.vgk.hr.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Изменение текста в заданной области PDF-документа")
public class TextEditRequest {

    @Schema(description = "Номер страницы PDF, начиная с 1", example = "1")
    private Integer pageNumber;

    @Schema(description = "Координата левого нижнего угла области по оси X", example = "150")
    private double x;
    @Schema(description = "Координата левого нижнего угла области по оси Y", example = "400")
    private double y;
    @Schema(description = "Ширина области текста", example = "200")
    private double width;
    @Schema(description = "Высота области текста", example = "30")
    private double height;
    @Schema(description = "Текст для подстановки", example = "Иванов Иван Иванович")
    private String text;

    @Builder.Default
    @Schema(description = "Размер шрифта в пунктах", example = "12", defaultValue = "12")
    private double fontSize = 12.0f;

    @Builder.Default
    @Schema(description = "Имя шрифта", example = "DEJAVU_SANS", defaultValue = "HELVETICA")
    private String fontName = "HELVETICA";

    @Builder.Default
    @Schema(description = "Жирное начертание шрифта", example = "false", defaultValue = "false")
    private Boolean bold = false;

    @Builder.Default
    @Schema(description = "Нужно ли закрыть прежний текст белым прямоугольником", defaultValue = "true")
    private Boolean overwrite = true;

    @Builder.Default
    @Schema(description = "Цвет текста в формате HEX", example = "#000000", defaultValue = "#000000")
    private String color = "#000000";
}
