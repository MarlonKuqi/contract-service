package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidClientException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CLIENT_INVALID";

    public InvalidClientException(String message) {
        super(message, DEFAULT_CODE);
    }

    public InvalidClientException(String message, Throwable cause) {
        super(message, DEFAULT_CODE, cause);
    }

    public static InvalidClientException forNullName() {
        return new InvalidClientException("ClientName must not be null");
    }

    public static InvalidClientException forNullEmail() {
        return new InvalidClientException("Email must not be null");
    }

    public static InvalidClientException forNullPhone() {
        return new InvalidClientException("ClientPhoneNumber must not be null");
    }

    public static InvalidClientException forNullBirthDate() {
        return new InvalidClientException("Birth date must not be null");
    }

    public static InvalidClientException forNullCompanyIdentifier() {
        return new InvalidClientException("Company identifier must not be null");
    }
}

