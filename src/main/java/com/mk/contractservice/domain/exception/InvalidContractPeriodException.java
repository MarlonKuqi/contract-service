package com.mk.contractservice.domain.exception;

public class InvalidContractPeriodException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_PERIOD_INVALID";

    public InvalidContractPeriodException(String message) {
        super(message, DEFAULT_CODE);
    }

    public static InvalidContractPeriodException forNullStartDate() {
        return new InvalidContractPeriodException("Start date must not be null");
    }

    public static InvalidContractPeriodException forNullEndDate() {
        return new InvalidContractPeriodException("End date must not be null");
    }

    public static InvalidContractPeriodException forEndBeforeStart() {
        return new InvalidContractPeriodException("End date must not be before start date");
    }
}

