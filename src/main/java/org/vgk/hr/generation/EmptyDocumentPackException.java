package org.vgk.hr.generation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmptyDocumentPackException extends RuntimeException {

    public EmptyDocumentPackException(Long positionId) {
        super("Position has no active PDF templates: " + positionId);
    }
}
