package org.vgk.hr.emailtemplate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/positions/{positionId}/email-template")
@RequiredArgsConstructor
@Tag(name = "Email templates", description = "Шаблон письма должности")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @Operation(summary = "Получить шаблон письма")
    @GetMapping
    public EmailTemplateResponse get(@PathVariable Long positionId) {
        return emailTemplateService.get(positionId);
    }

    @Operation(summary = "Создать или обновить шаблон письма")
    @PutMapping
    public EmailTemplateResponse upsert(
            @PathVariable Long positionId,
            @RequestBody EmailTemplateRequest request
    ) {
        return emailTemplateService.upsert(positionId, request);
    }
}
