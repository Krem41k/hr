package org.vgk.hr.emailtemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Подставляет значения в шаблон письма по плейсхолдерам {{fieldCode}}.
 * Плейсхолдер без значения заменяется пустой строкой.
 */
@Component
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    public String render(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            String value = values.get(code);
            if (value == null) {
                log.warn("Email template placeholder has no value: fieldCode={}", code);
                value = "";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
