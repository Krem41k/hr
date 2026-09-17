package org.vgk.hr.template;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FieldCodeConflictException extends RuntimeException {

    public FieldCodeConflictException(String fieldCode, String detail) {
        super("Conflicting fieldCode '%s': %s".formatted(fieldCode, detail));
    }
}
