package com.mk.contractservice.domain.exception;

public class InvalidPhoneNumberException extends DomainValidationException {

    private static final String DEFAULT_CODE = "PHONE_NUMBER_INVALID";

    public InvalidPhoneNumberException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidPhoneNumberException forNull() {
        return new InvalidPhoneNumberException("Phone number must not be null");
    }

    public static InvalidPhoneNumberException forBlank() {
        return new InvalidPhoneNumberException("Phone number must not be blank");
    }

    public static InvalidPhoneNumberException forInvalidFormat(String value) {
        return new InvalidPhoneNumberException("Invalid phone number format (must start with +): " + value);
    }
}

