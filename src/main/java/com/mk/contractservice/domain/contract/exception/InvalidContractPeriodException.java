package com.mk.contractservice.domain.contract.exception;

import com.mk.contractservice.domain.exception.DomainValidationException;

public class InvalidContractPeriodException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_PERIOD_INVALID";

    public InvalidContractPeriodException(String message) {
        super(message, DEFAULT_CODE);
    }
}

