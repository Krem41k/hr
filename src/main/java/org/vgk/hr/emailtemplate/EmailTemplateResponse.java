package org.vgk.hr.emailtemplate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Шаблон письма")
public record EmailTemplateResponse(
        @Schema(description = "Идентификатор") Long id,
        @Schema(description = "Идентификатор должности") Long positionId,
        @Schema(description = "Тема") String subjectTemplate,
        @Schema(description = "Тело") String bodyTemplate
) {
    public static EmailTemplateResponse from(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getPosition().getId(),
                template.getSubjectTemplate(),
                template.getBodyTemplate()
        );
    }
}
