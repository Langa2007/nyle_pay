package com.nyle.nylepay.exceptions;

/**
 * Base exception for NylePay application errors that are safe to show to the user.
 */
public class NylePayException extends RuntimeException {
    public NylePayException(String message) {
        super(message);
    }

    public NylePayException(String message, Throwable cause) {
        super(message, cause);
    }
}
