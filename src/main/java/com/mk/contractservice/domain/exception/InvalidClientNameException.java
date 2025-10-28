package com.mk.contractservice.domain.exception;

public class InvalidClientNameException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CLIENT_NAME_INVALID";

    public InvalidClientNameException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidClientNameException forNull() {
        return new InvalidClientNameException("Client name must not be null");
    }

    public static InvalidClientNameException forBlank() {
        return new InvalidClientNameException("Client name must not be blank");
    }

    public static InvalidClientNameException forTooLong(int maxLength) {
        return new InvalidClientNameException("Client name must not exceed " + maxLength + " characters");
    }
}

