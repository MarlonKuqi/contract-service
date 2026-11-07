package com.mk.contractservice.domain.contract.exception;

public final class InvalidContractCostException extends ContractValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_COST_INVALID";

    public InvalidContractCostException(String message) {
        super(message, DEFAULT_CODE);
    }
}

