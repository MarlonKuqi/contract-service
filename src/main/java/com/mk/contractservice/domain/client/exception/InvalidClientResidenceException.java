package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidClientResidenceException extends DomainValidationException {

    private static final String CODE = "invalidClientResidence";

    public InvalidClientResidenceException(final String message) {
        super(message, CODE);
    }
}
