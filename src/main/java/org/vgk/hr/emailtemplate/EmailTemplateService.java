package org.vgk.hr.emailtemplate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgk.hr.position.Position;
import org.vgk.hr.position.PositionService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final PositionService positionService;

    @Transactional(readOnly = true)
    public EmailTemplateResponse get(Long positionId) {
        positionService.requirePosition(positionId);
        return EmailTemplateResponse.from(emailTemplateRepository.findByPositionId(positionId)
                .orElseThrow(() -> new EmailTemplateNotFoundException(positionId)));
    }

    /** Шаблон письма должности, если он настроен: генерация пакета не должна падать без письма. */
    @Transactional(readOnly = true)
    public Optional<EmailTemplateResponse> find(Long positionId) {
        return emailTemplateRepository.findByPositionId(positionId).map(EmailTemplateResponse::from);
    }

    @Transactional
    public EmailTemplateResponse upsert(Long positionId, EmailTemplateRequest request) {
        validate(request);
        Position position = positionService.requirePosition(positionId);
        EmailTemplate template = emailTemplateRepository.findByPositionId(positionId)
                .orElseGet(() -> {
                    EmailTemplate created = new EmailTemplate(position, request.subjectTemplate(), request.bodyTemplate());
                    position.setEmailTemplate(created);
                    return created;
                });
        template.setSubjectTemplate(request.subjectTemplate().trim());
        template.setBodyTemplate(request.bodyTemplate());
        return EmailTemplateResponse.from(emailTemplateRepository.save(template));
    }

    private void validate(EmailTemplateRequest request) {
        if (request.subjectTemplate() == null || request.subjectTemplate().isBlank()) {
            throw new IllegalArgumentException("Email subject template must not be blank");
        }
        if (request.bodyTemplate() == null) {
            throw new IllegalArgumentException("Email body template must not be null");
        }
    }
}
