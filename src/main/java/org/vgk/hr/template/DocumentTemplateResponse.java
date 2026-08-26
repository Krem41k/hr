package org.vgk.hr.template;

import io.swagger.v3.oas.annotations.media.Schema;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.db.entity.TemplateField;

import java.util.List;

@Schema(description = "PDF-шаблон должности без бинарного содержимого")
public record DocumentTemplateResponse(
        @Schema(description = "Идентификатор") Long id,
        @Schema(description = "Идентификатор должности") Long positionId,
        @Schema(description = "Имя") String name,
        @Schema(description = "Порядок") int sortOrder,
        @Schema(description = "Исходное имя файла") String originalFileName,
        @Schema(description = "Legacy номер шаблона") Integer templateNumber,
        @Schema(description = "Активные поля") List<DocumentTemplateFieldResponse> fields
) {
    public static DocumentTemplateResponse from(PdfTemplate template) {
        return new DocumentTemplateResponse(
                template.getId(),
                template.getPosition() == null ? null : template.getPosition().getId(),
                template.getName(),
                template.getSortOrder(),
                template.getOriginalFileName(),
                template.getTemplateNumber(),
                template.getActiveFields().stream().map(DocumentTemplateFieldResponse::from).toList()
        );
    }

    @Schema(description = "Поле PDF-шаблона")
    public record DocumentTemplateFieldResponse(
            Long id,
            String fieldCode,
            String label,
            FieldValueType valueType,
            FieldValueSource valueSource,
            int pageNumber,
            double x,
            double y,
            double width,
            double height,
            double fontSize,
            String fontName,
            String color,
            boolean bold,
            String dateFormat
    ) {
        public static DocumentTemplateFieldResponse from(TemplateField field) {
            return new DocumentTemplateFieldResponse(
                    field.getId(),
                    field.getFieldCode(),
                    field.getLabel(),
                    field.getValueType(),
                    field.getValueSource(),
                    field.getPageNumber(),
                    field.getX(),
                    field.getY(),
                    field.getWidth(),
                    field.getHeight(),
                    field.getFontSize(),
                    field.getFontName(),
                    field.getColor(),
                    field.isBold(),
                    field.getDateFormat()
            );
        }
    }
}
