package com.mk.contractservice.domain.client.exception;


import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidClientEmailException extends DomainValidationException {

    public static final String DEFAULT_CODE = "EMAIL_INVALID";

    public InvalidClientEmailException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidClientEmailException forBlank() {
        return new InvalidClientEmailException("Email must not be blank");
    }

    public static InvalidClientEmailException forTooLong(int maxLength) {
        return new InvalidClientEmailException("Email too long (max " + maxLength + " characters per RFC 5321)");
    }

    public static InvalidClientEmailException forInvalidFormat() {
        return new InvalidClientEmailException("Invalid email format");
    }
}
