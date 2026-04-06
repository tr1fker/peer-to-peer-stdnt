package com.peerlending.common.exception;

/**
 * Wrong credentials or account cannot use password login — mapped to HTTP 401.
 */
public class InvalidLoginException extends RuntimeException {

    public InvalidLoginException(String message) {
        super(message);
    }
}
