package com.mk.contractservice.domain.contract.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public abstract sealed class ContractValidationException extends DomainValidationException
        permits InvalidContractCostException, InvalidContractPeriodException {

    protected ContractValidationException(final String message, final String code) {
        super(message, code);
    }
}
