package org.vgk.hr.generation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collection;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownFieldCodesException extends RuntimeException {

    public UnknownFieldCodesException(Collection<String> fieldCodes) {
        super("Unknown fieldCodes for position form: " + String.join(", ", fieldCodes));
    }
}
