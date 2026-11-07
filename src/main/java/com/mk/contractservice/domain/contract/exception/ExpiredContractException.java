package com.mk.contractservice.domain.contract.exception;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ExpiredContractException extends RuntimeException {

    public ExpiredContractException(@Nullable UUID contractId) {
        super(String.format("Cannot modify expired contract: %s", contractId));
    }

    public ExpiredContractException(String message) {
        super(message);
    }
}

