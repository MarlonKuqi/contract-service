package com.mk.contractservice.domain.contract.exception;

import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.shared.ClientKind;

import java.util.Locale;

public final class ContractTypeNotAllowedException extends ContractValidationException {

    private static final String DEFAULT_CODE = "CONTRACT_TYPE_NOT_ALLOWED";

    public ContractTypeNotAllowedException(final ContractType type, final ClientKind clientKind) {
        super("A " + type + " contract cannot be taken out by a " + clientKind.name().toLowerCase(Locale.ROOT),
                DEFAULT_CODE);
    }
}
