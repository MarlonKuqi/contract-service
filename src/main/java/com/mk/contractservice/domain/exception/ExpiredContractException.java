package com.mk.contractservice.domain.exception;

import java.util.UUID;

public class ExpiredContractException extends RuntimeException {

    public ExpiredContractException(UUID contractId) {
        super(String.format("Cannot modify expired contract: %s", contractId));
    }

    public ExpiredContractException(String message) {
        super(message);
    }
}

