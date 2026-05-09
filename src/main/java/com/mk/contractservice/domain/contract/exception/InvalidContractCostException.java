package com.mk.contractservice.domain.contract.exception;


import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidContractCostException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_COST_INVALID";

    public InvalidContractCostException(String message) {
        super(message, DEFAULT_CODE);
    }
}

