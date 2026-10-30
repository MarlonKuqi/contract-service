package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidClientPhoneNumberException extends DomainValidationException {

    private static final String DEFAULT_CODE = "PHONE_INVALID";

    public InvalidClientPhoneNumberException(String message) {
        super(message, DEFAULT_CODE);
    }
}

