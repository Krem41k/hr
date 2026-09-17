package org.vgk.hr.generation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collection;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingFieldValuesException extends RuntimeException {

    public MissingFieldValuesException(Collection<String> fieldCodes) {
        super("Missing values for required fields: " + String.join(", ", fieldCodes));
    }
}
