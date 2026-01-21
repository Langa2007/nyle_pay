package com.nyle.nylepay.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String currency, Double required, Double available) {
        super(String.format("Insufficient %s balance. Required: %s, Available: %s",
                currency, required, available));
    }
}
