package com.mk.contractservice.domain.exception;

public class InvalidPersonBirthDateException extends DomainValidationException {

    private static final String DEFAULT_CODE = "BIRTH_DATE_INVALID";

    public InvalidPersonBirthDateException(String message) {
        super(message, DEFAULT_CODE);
    }
}

