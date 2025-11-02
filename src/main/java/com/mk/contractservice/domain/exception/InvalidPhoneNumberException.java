package com.mk.contractservice.domain.exception;

public class InvalidPhoneNumberException extends DomainValidationException {

    private static final String DEFAULT_CODE = "PHONE_INVALID";

    public InvalidPhoneNumberException(String message) {
        super(message, DEFAULT_CODE);
    }
}

