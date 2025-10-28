package com.mk.contractservice.domain.exception;

public class InvalidPersonBirthDateException extends DomainValidationException {

    private static final String DEFAULT_CODE = "PERSON_BIRTHDATE_INVALID";

    public InvalidPersonBirthDateException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidPersonBirthDateException forNull() {
        return new InvalidPersonBirthDateException("Birth date must not be null");
    }

    public static InvalidPersonBirthDateException forFutureDate() {
        return new InvalidPersonBirthDateException("Birth date must not be in the future");
    }
}

