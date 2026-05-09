package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidCompanyIdentifierException extends DomainValidationException {

    private static final String DEFAULT_CODE = "COMPANY_IDENTIFIER_INVALID";

    public InvalidCompanyIdentifierException(String message) {
        super(message, DEFAULT_CODE);
    }
}

