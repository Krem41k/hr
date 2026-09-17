package org.vgk.hr.emailtemplate;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void replacesPlaceholdersIncludingSpacedForm() {
        String result = renderer.render(
                "Здравствуйте, {{fullName}}! Дата рождения: {{ birthDate }}.",
                Map.of("fullName", "Иванов Иван", "birthDate", "15.05.1990")
        );

        assertEquals("Здравствуйте, Иванов Иван! Дата рождения: 15.05.1990.", result);
    }

    @Test
    void replacesUnknownPlaceholderWithEmptyString() {
        assertEquals("Тема: ", renderer.render("Тема: {{unknown}}", Map.of()));
    }

    @Test
    void keepsDollarAndBackslashFromValues() {
        assertEquals("$1\\x", renderer.render("{{code}}", Map.of("code", "$1\\x")));
    }

    @Test
    void returnsEmptyStringForBlankTemplate() {
        assertEquals("", renderer.render(null, Map.of()));
        assertEquals("", renderer.render("", Map.of()));
    }
}
