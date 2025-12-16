package com.mk.contractservice.domain.client.exception;

import com.mk.contractservice.domain.exception.DomainValidationException;

public class InvalidPersonBirthDateException extends DomainValidationException {

    private static final String DEFAULT_CODE = "BIRTH_DATE_INVALID";

    public InvalidPersonBirthDateException(String message) {
        super(message, DEFAULT_CODE);
    }
}

