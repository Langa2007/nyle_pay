package com.nyle.nylepay.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends NylePayException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
