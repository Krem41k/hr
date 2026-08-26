package org.vgk.hr.shared;

import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;
import org.vgk.hr.domain.request.TemplateFieldRequest;

public final class TemplateFieldMapper {

    private TemplateFieldMapper() {
    }

    public static TemplateField toEntity(TemplateFieldRequest request) {
        String fieldCode = WellKnownFieldCodes.normalize(request.fieldCode());
        FieldValueType valueType = request.valueType() != null
                ? request.valueType()
                : defaultValueType(fieldCode);
        FieldValueSource valueSource = request.valueSource() != null
                ? request.valueSource()
                : defaultValueSource(fieldCode);
        String label = request.label() == null || request.label().isBlank()
                ? fieldCode
                : request.label();
        return new TemplateField(
                fieldCode,
                label,
                valueType,
                valueSource,
                request.pageNumber(),
                request.x(),
                request.y(),
                request.width(),
                request.height(),
                request.fontSize(),
                request.fontName(),
                request.color(),
                Boolean.TRUE.equals(request.bold()),
                request.dateFormat()
        );
    }

    public static FieldValueType defaultValueType(String fieldCode) {
        if (WellKnownFieldCodes.BIRTH_DATE.equals(fieldCode)
                || WellKnownFieldCodes.CURRENT_DATE.equals(fieldCode)) {
            return FieldValueType.DATE;
        }
        return FieldValueType.TEXT;
    }

    public static FieldValueSource defaultValueSource(String fieldCode) {
        if (WellKnownFieldCodes.CURRENT_DATE.equals(fieldCode)) {
            return FieldValueSource.SYSTEM;
        }
        return FieldValueSource.USER;
    }
}
