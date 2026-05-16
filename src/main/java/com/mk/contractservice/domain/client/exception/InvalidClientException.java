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
}

