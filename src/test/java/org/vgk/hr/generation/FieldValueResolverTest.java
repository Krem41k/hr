package org.vgk.hr.generation;

import org.junit.jupiter.api.Test;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldValueResolverTest {

    private final FieldValueResolver resolver = new FieldValueResolver();

    @Test
    void formatsUserDateWithTemplateFormat() {
        TemplateField field = field(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, FieldValueSource.USER, "dd/MM/yyyy");

        String value = resolver.forPdf(field, Map.of(WellKnownFieldCodes.BIRTH_DATE, "1990-05-15"), LocalDate.now());

        assertEquals("15/05/1990", value);
    }

    @Test
    void fallsBackToDefaultDateFormat() {
        TemplateField field = field(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, FieldValueSource.USER, null);

        String value = resolver.forPdf(field, Map.of(WellKnownFieldCodes.BIRTH_DATE, "1990-05-15"), LocalDate.now());

        assertEquals("15.05.1990", value);
    }

    @Test
    void resolvesSystemCurrentDateFromServerClock() {
        TemplateField field = field(WellKnownFieldCodes.CURRENT_DATE, FieldValueType.DATE, FieldValueSource.SYSTEM, "dd.MM.yyyy");

        String value = resolver.forPdf(field, Map.of(), LocalDate.of(2026, 9, 17));

        assertEquals("17.09.2026", value);
    }

    @Test
    void passesTextValueThrough() {
        TemplateField field = field(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, FieldValueSource.USER, null);

        String value = resolver.forPdf(field, Map.of(WellKnownFieldCodes.FULL_NAME, "Иванов Иван"), LocalDate.now());

        assertEquals("Иванов Иван", value);
    }

    @Test
    void rejectsNonIsoDate() {
        TemplateField field = field(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, FieldValueSource.USER, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resolver.forPdf(field, Map.of(WellKnownFieldCodes.BIRTH_DATE, "15.05.1990"), LocalDate.now()));

        assertEquals("Field 'birthDate' must be an ISO date (yyyy-MM-dd), got '15.05.1990'", ex.getMessage());
    }

    @Test
    void rejectsUnsupportedSystemFieldCode() {
        TemplateField field = field("managerName", FieldValueType.TEXT, FieldValueSource.SYSTEM, null);

        assertThrows(IllegalArgumentException.class, () -> resolver.forPdf(field, Map.of(), LocalDate.now()));
    }

    @Test
    void formatsEmailDatesWithDefaultFormat() {
        assertEquals("15.05.1990",
                resolver.forEmail(WellKnownFieldCodes.BIRTH_DATE, FieldValueType.DATE, "1990-05-15"));
        assertEquals("Иванов",
                resolver.forEmail(WellKnownFieldCodes.FULL_NAME, FieldValueType.TEXT, "Иванов"));
    }

    private TemplateField field(String code, FieldValueType type, FieldValueSource source, String dateFormat) {
        return new TemplateField(code, code, type, source, 1, 10, 20, 100, 20, 12, "DEJAVU_SANS", "#000", false, dateFormat);
    }
}
