package com.mk.contractservice.domain.client.exception;


import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidClientEmailException extends DomainValidationException {

    private static final String DEFAULT_CODE = "EMAIL_INVALID";

    public InvalidClientEmailException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidClientEmailException forBlank() {
        return new InvalidClientEmailException("Email must not be blank");
    }

    public static InvalidClientEmailException forInvalidFormat(String value) {
        return new InvalidClientEmailException("Invalid email format: " + value);
    }
}

