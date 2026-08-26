package org.vgk.hr.template;

import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Проверяет, что одинаковые fieldCode в пакете должности имеют совместимые label/valueType.
 */
public final class FieldCodeConflictValidator {

    private FieldCodeConflictValidator() {
    }

    public static void validateCompatible(List<TemplateField> existingFields, List<TemplateField> incomingFields) {
        Map<String, TemplateField> byCode = new HashMap<>();
        for (TemplateField field : existingFields) {
            merge(byCode, field);
        }
        for (TemplateField field : incomingFields) {
            merge(byCode, field);
        }
    }

    private static void merge(Map<String, TemplateField> byCode, TemplateField field) {
        String code = WellKnownFieldCodes.normalize(field.getFieldCode());
        TemplateField previous = byCode.putIfAbsent(code, field);
        if (previous == null) {
            return;
        }
        if (previous.getValueType() != field.getValueType()) {
            throw new FieldCodeConflictException(
                    code,
                    "valueType differs (%s vs %s)".formatted(previous.getValueType(), field.getValueType())
            );
        }
        if (!Objects.equals(normalizeLabel(previous.getLabel()), normalizeLabel(field.getLabel()))) {
            throw new FieldCodeConflictException(
                    code,
                    "label differs ('%s' vs '%s')".formatted(previous.getLabel(), field.getLabel())
            );
        }
        if (previous.getValueSource() != field.getValueSource()) {
            throw new FieldCodeConflictException(
                    code,
                    "valueSource differs (%s vs %s)".formatted(previous.getValueSource(), field.getValueSource())
            );
        }
    }

    private static String normalizeLabel(String label) {
        return label == null ? "" : label.trim();
    }
}
