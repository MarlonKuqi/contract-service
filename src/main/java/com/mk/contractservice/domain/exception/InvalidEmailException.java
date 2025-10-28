package com.mk.contractservice.domain.exception;

public class InvalidEmailException extends DomainValidationException {

    private static final String DEFAULT_CODE = "EMAIL_INVALID";

    public InvalidEmailException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidEmailException forNull() {
        return new InvalidEmailException("Email must not be null");
    }

    public static InvalidEmailException forBlank() {
        return new InvalidEmailException("Email must not be blank");
    }

    public static InvalidEmailException forInvalidFormat(String value) {
        return new InvalidEmailException("Invalid email format: " + value);
    }
}

