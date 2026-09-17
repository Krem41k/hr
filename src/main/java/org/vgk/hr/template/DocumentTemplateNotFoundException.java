package org.vgk.hr.template;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentTemplateNotFoundException extends RuntimeException {

    public DocumentTemplateNotFoundException(Long positionId, Long templateId) {
        super("Document template not found: positionId=%d, templateId=%d".formatted(positionId, templateId));
    }
}
