package com.mk.contractservice.domain.exception;

import java.util.UUID;

public class ContractNotOwnedByClientException extends RuntimeException {

    public ContractNotOwnedByClientException(UUID contractId, UUID expectedClientId) {
        super(String.format("Contract %s does not belong to client %s", contractId, expectedClientId));
    }
}

