package org.vgk.hr.domain.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Параметры PDF-шаблона")
public record TemplateUploadRequest(
        @Schema(description = "Уникальный номер шаблона", example = "1") Integer templateNumber,
        @Schema(description = "Области подстановки с fieldCode и координатами", requiredMode = Schema.RequiredMode.REQUIRED)
        List<TemplateFieldRequest> fields
) {
}
