package com.mk.contractservice.domain.exception;

public class InvalidContractCostException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_COST_INVALID";

    public InvalidContractCostException(String message) {
        super(message, DEFAULT_CODE);
    }

    public InvalidContractCostException(String message, Throwable cause) {
        super(message, DEFAULT_CODE, cause);
    }

    public static InvalidContractCostException forNull() {
        return new InvalidContractCostException("Contract cost amount must not be null");
    }

    public static InvalidContractCostException forNegative(String value) {
        return new InvalidContractCostException("Contract cost amount must not be negative: " + value);
    }

    public static InvalidContractCostException forTooManyDecimals(String value) {
        return new InvalidContractCostException("Contract cost amount must have at most 2 decimal places: " + value);
    }
}

