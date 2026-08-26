package org.vgk.hr.db.entity;

/**
 * Известные коды полей, которые поддерживает текущий API генерации.
 * Произвольные fieldCode можно сохранять в шаблоне; подстановка для них появится позже.
 */
public final class WellKnownFieldCodes {

    public static final String FULL_NAME = "fullName";
    public static final String BIRTH_DATE = "birthDate";
    public static final String CURRENT_DATE = "currentDate";

    private WellKnownFieldCodes() {
    }

    public static String normalize(String fieldCode) {
        if (fieldCode == null || fieldCode.isBlank()) {
            return fieldCode;
        }
        String trimmed = fieldCode.trim();
        return switch (trimmed.toUpperCase()) {
            case "FULL_NAME", "FULLNAME" -> FULL_NAME;
            case "BIRTH_DATE", "BIRTHDATE" -> BIRTH_DATE;
            case "CURRENT_DATE", "CURRENTDATE" -> CURRENT_DATE;
            default -> Character.toLowerCase(trimmed.charAt(0)) + (trimmed.length() > 1 ? trimmed.substring(1) : "");
        };
    }
}
