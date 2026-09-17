package org.vgk.hr.emailtemplate;

import org.junit.jupiter.api.Test;
import org.vgk.hr.position.Position;
import org.vgk.hr.position.PositionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailTemplateServiceTest {

    private final EmailTemplateRepository repository = mock(EmailTemplateRepository.class);
    private final PositionService positionService = mock(PositionService.class);
    private final EmailTemplateService service = new EmailTemplateService(repository, positionService);

    @Test
    void upsertsNewEmailTemplate() {
        Position position = new Position("Водитель", "driver");
        position.setId(1L);
        when(positionService.requirePosition(1L)).thenReturn(position);
        when(repository.findByPositionId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(EmailTemplate.class))).thenAnswer(invocation -> {
            EmailTemplate template = invocation.getArgument(0);
            template.setId(7L);
            return template;
        });

        EmailTemplateResponse response = service.upsert(
                1L,
                new EmailTemplateRequest("Тема {{fullName}}", "Тело {{fullName}}")
        );

        assertEquals(7L, response.id());
        assertEquals(1L, response.positionId());
        assertEquals("Тема {{fullName}}", response.subjectTemplate());
        assertEquals("Тело {{fullName}}", response.bodyTemplate());
    }

    @Test
    void updatesExistingEmailTemplate() {
        Position position = new Position("Водитель", "driver");
        position.setId(1L);
        EmailTemplate existing = new EmailTemplate(position, "old", "old body");
        existing.setId(3L);
        when(positionService.requirePosition(1L)).thenReturn(position);
        when(repository.findByPositionId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        EmailTemplateResponse response = service.upsert(1L, new EmailTemplateRequest("new", "new body"));

        assertEquals("new", response.subjectTemplate());
        assertEquals("new body", response.bodyTemplate());
    }
}
