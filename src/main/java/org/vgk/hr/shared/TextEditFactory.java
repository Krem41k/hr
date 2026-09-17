package org.vgk.hr.shared;

import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.domain.request.TextEditRequest;

/**
 * Собирает запрос на правку PDF из настроек поля шаблона и готового значения.
 */
public final class TextEditFactory {

    private TextEditFactory() {
    }

    public static TextEditRequest from(TemplateField field, String text) {
        return TextEditRequest.builder()
                .pageNumber(field.getPageNumber())
                .x(field.getX())
                .y(field.getY())
                .width(field.getWidth())
                .height(field.getHeight())
                .text(text)
                .fontSize(field.getFontSize())
                .fontName(field.getFontName())
                .bold(field.isBold())
                .color(field.getColor())
                .overwrite(true)
                .build();
    }
}
