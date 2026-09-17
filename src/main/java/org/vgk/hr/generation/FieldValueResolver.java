package org.vgk.hr.generation;

import org.springframework.stereotype.Component;
import org.vgk.hr.db.entity.FieldValueSource;
import org.vgk.hr.db.entity.FieldValueType;
import org.vgk.hr.db.entity.TemplateField;
import org.vgk.hr.db.entity.WellKnownFieldCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Приводит значения из запроса оператора к тексту для подстановки в PDF и в письмо.
 * DATE-поля приходят в ISO (yyyy-MM-dd) и форматируются по настройке шаблона.
 */
@Component
public class FieldValueResolver {

    /** Формат даты в письме, когда у поля нет собственного формата. */
    public static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy";

    private static final Locale RU = Locale.forLanguageTag("ru");

    public String forPdf(TemplateField field, Map<String, String> userValues, LocalDate today) {
        String code = WellKnownFieldCodes.normalize(field.getFieldCode());
        String rawValue = field.getValueSource() == FieldValueSource.SYSTEM
                ? systemValue(code, today)
                : userValues.get(code);
        if (field.getValueType() == FieldValueType.DATE) {
            return formatDate(parseDate(code, rawValue), field.getDateFormat());
        }
        return rawValue;
    }

    public String forEmail(String fieldCode, FieldValueType valueType, String rawValue) {
        if (valueType == FieldValueType.DATE) {
            return formatDate(parseDate(fieldCode, rawValue), DEFAULT_DATE_FORMAT);
        }
        return rawValue;
    }

    private String systemValue(String code, LocalDate today) {
        if (WellKnownFieldCodes.CURRENT_DATE.equals(code)) {
            return today.toString();
        }
        throw new IllegalArgumentException("Unsupported SYSTEM fieldCode: " + code);
    }

    private LocalDate parseDate(String code, String rawValue) {
        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Field '%s' must be an ISO date (yyyy-MM-dd), got '%s'".formatted(code, rawValue)
            );
        }
    }

    private String formatDate(LocalDate date, String dateFormat) {
        String pattern = dateFormat == null || dateFormat.isBlank() ? DEFAULT_DATE_FORMAT : dateFormat;
        return date.format(DateTimeFormatter.ofPattern(pattern, RU));
    }
}
