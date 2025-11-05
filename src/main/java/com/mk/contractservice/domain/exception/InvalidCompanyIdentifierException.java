package com.mk.contractservice.domain.exception;

public class InvalidCompanyIdentifierException extends DomainValidationException {

    private static final String DEFAULT_CODE = "COMPANY_IDENTIFIER_INVALID";

    public InvalidCompanyIdentifierException(String message) {
        super(message, DEFAULT_CODE);
    }
}

