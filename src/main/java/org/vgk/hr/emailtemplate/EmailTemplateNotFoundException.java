package org.vgk.hr.emailtemplate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EmailTemplateNotFoundException extends RuntimeException {

    public EmailTemplateNotFoundException(Long positionId) {
        super("Email template not found for position: " + positionId);
    }
}
