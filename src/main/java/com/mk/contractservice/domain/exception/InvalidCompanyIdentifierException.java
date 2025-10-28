package com.mk.contractservice.domain.exception;

public class InvalidCompanyIdentifierException extends DomainValidationException {

    private static final String DEFAULT_CODE = "COMPANY_IDENTIFIER_INVALID";

    public InvalidCompanyIdentifierException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidCompanyIdentifierException forNull() {
        return new InvalidCompanyIdentifierException("Company identifier must not be null");
    }

    public static InvalidCompanyIdentifierException forBlank() {
        return new InvalidCompanyIdentifierException("Company identifier must not be blank");
    }

    public static InvalidCompanyIdentifierException forInvalidFormat(String value) {
        return new InvalidCompanyIdentifierException("Invalid company identifier format (expected CHE-XXX.XXX.XXX): " + value);
    }
}

