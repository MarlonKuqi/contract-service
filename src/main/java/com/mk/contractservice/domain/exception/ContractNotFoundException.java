package com.mk.contractservice.domain.exception;

import java.util.UUID;

public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException(String message) {
        super(message);
    }

    public ContractNotFoundException(UUID contractId) {
        super("Contract not found: " + contractId);
    }
}

