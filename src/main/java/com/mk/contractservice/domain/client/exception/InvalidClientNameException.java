package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.exception.DomainValidationException;

public class InvalidClientNameException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CLIENT_NAME_INVALID";

    public InvalidClientNameException(String message) {
        super(message, DEFAULT_CODE);
    }
}

