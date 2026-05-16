package com.mk.contractservice.domain.contract.exception;


import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidContractException extends DomainValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_INVALID";

    public InvalidContractException(String message) {
        super(message, DEFAULT_CODE);
    }

    public InvalidContractException(String message, Throwable cause) {
        super(message, DEFAULT_CODE, cause);
    }
}

