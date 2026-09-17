package org.vgk.hr.emailtemplate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Шаблон письма должности")
public record EmailTemplateRequest(
        @Schema(description = "Тема письма с плейсхолдерами {{fieldCode}}", example = "Документы для {{fullName}}")
        String subjectTemplate,
        @Schema(description = "Тело письма с плейсхолдерами {{fieldCode}}", example = "Здравствуйте, {{fullName}}!")
        String bodyTemplate
) {
}
