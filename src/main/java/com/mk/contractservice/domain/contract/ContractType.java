package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.ClientKind;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ContractType {
    LAMAL, LAA, LPP;

    public static Set<ContractType> forbiddingOverlap() {
        return Arrays.stream(values())
                .filter(ContractType::forbidsOverlap)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean forbidsOverlap() {
        return this == LAMAL;
    }

    public boolean isClosedTo(final ClientKind clientKind) {
        return switch (clientKind) {
            case PERSON -> this != LAMAL;
            case COMPANY -> this != LAA && this != LPP;
        };
    }
}
