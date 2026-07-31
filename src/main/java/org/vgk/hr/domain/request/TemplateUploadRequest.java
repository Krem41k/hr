package org.vgk.hr.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Параметры PDF-шаблона")
public record TemplateUploadRequest(
        @Schema(description = "Уникальный номер шаблона", example = "1") Integer templateNumber,
        @Schema(description = "Все области, в которые подставляются дата, дата рождения или ФИО", requiredMode = Schema.RequiredMode.REQUIRED)
        List<TemplateFieldRequest> fields
) {
}
