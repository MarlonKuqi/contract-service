package com.mk.contractservice.domain.contract.exception;

public final class InvalidContractFranchiseException extends ContractValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_FRANCHISE_INVALID";

    public InvalidContractFranchiseException(final String message) {
        super(message, DEFAULT_CODE);
    }
}
