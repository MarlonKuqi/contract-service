package com.mk.contractservice.domain.exception;

public class InvalidContractException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_INVALID";

    public InvalidContractException(String message) {
        super(message, DEFAULT_CODE);
    }

    public InvalidContractException(String message, Throwable cause) {
        super(message, DEFAULT_CODE, cause);
    }

    public static InvalidContractException forNullClient() {
        return new InvalidContractException("Client cannot be null for a contract");
    }

    public static InvalidContractException forNullPeriod() {
        return new InvalidContractException("Contract period cannot be null");
    }

    public static InvalidContractException forNullCostAmount() {
        return new InvalidContractException("Cost amount cannot be null");
    }

    public static InvalidContractException forNullNewCostAmount() {
        return new InvalidContractException("New cost amount cannot be null");
    }
}

