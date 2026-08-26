package org.vgk.hr.position;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PositionNotFoundException extends RuntimeException {

    public PositionNotFoundException(Long id) {
        super("Position not found: " + id);
    }
}
