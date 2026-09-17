package org.vgk.hr.generation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PositionInactiveException extends RuntimeException {

    public PositionInactiveException(Long positionId) {
        super("Position is not active: " + positionId);
    }
}
