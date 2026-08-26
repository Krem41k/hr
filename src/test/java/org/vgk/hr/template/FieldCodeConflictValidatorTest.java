package org.vgk.hr.template;

import org.junit.jupiter.api.Test;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldCodeConflictValidatorTest {

    @Test
    void allowsSameCodeWithSameLabelAndType() {
        assertDoesNotThrow(() -> FieldCodeConflictValidator.validateCompatible(
                List.of(field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER)),
                List.of(field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER))
        ));
    }

    @Test
    void rejectsDifferentLabels() {
        FieldCodeConflictException ex = assertThrows(FieldCodeConflictException.class, () ->
                FieldCodeConflictValidator.validateCompatible(
                        List.of(field(WellKnownFieldCodes.FULL_NAME, "ФИО", FieldValueType.TEXT, FieldValueSource.USER)),
                        List.of(field(WellKnownFieldCodes.FULL_NAME, "Фамилия Имя", FieldValueType.TEXT, FieldValueSource.USER))
                ));
        assertTrue(ex.getMessage().contains("fullName"));
        assertTrue(ex.getMessage().contains("label"));
    }

    @Test
    void rejectsDifferentValueTypes() {
        assertThrows(FieldCodeConflictException.class, () ->
                FieldCodeConflictValidator.validateCompatible(
                        List.of(field(WellKnownFieldCodes.BIRTH_DATE, "ДР", FieldValueType.DATE, FieldValueSource.USER)),
                        List.of(field(WellKnownFieldCodes.BIRTH_DATE, "ДР", FieldValueType.TEXT, FieldValueSource.USER))
                ));
    }

    private TemplateField field(String code, String label, FieldValueType type, FieldValueSource source) {
        return new TemplateField(code, label, type, source, 1, 1, 1, 1, 1, 12, "DEJAVU_SANS", "#000", false, null);
    }
}
