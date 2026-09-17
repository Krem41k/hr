package org.vgk.hr.template;

import io.swagger.v3.oas.annotations.media.Schema;
import org.vgk.hr.domain.request.TemplateFieldRequest;

import java.util.List;

@Schema(description = "Метаданные PDF-шаблона в пакете должности")
public record DocumentTemplateUploadRequest(
        @Schema(description = "Отображаемое имя шаблона", example = "Направление")
        String name,
        @Schema(description = "Порядок в пакете", example = "0")
        Integer sortOrder,
        @Schema(description = "Области подстановки", requiredMode = Schema.RequiredMode.REQUIRED)
        List<TemplateFieldRequest> fields
) {
}
