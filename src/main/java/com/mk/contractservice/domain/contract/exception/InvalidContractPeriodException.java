package com.mk.contractservice.domain.contract.exception;

public final class InvalidContractPeriodException extends ContractValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_PERIOD_INVALID";

    public InvalidContractPeriodException(String message) {
        super(message, DEFAULT_CODE);
    }
}
