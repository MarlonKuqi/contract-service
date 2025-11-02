package com.mk.contractservice.domain.exception;

public class InvalidContractPeriodException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_PERIOD_INVALID";

    public InvalidContractPeriodException(String message) {
        super(message, DEFAULT_CODE);
    }
}

