package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.ClientKind;

public enum ContractType {
    LAMAL, LAA, LPP;

    public boolean isClosedTo(final ClientKind clientKind) {
        return switch (clientKind) {
            case PERSON -> this != LAMAL;
            case COMPANY -> this != LAA && this != LPP;
        };
    }
}
