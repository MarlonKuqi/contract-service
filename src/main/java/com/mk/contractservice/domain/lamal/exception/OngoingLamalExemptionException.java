package com.mk.contractservice.domain.lamal.exception;

import com.mk.contractservice.domain.lamal.LamalExemption;

import java.util.Objects;
import java.util.UUID;

public class OngoingLamalExemptionException extends RuntimeException {

    public OngoingLamalExemptionException(final UUID personId, final LamalExemption exemption) {
        super("Person " + personId + " is exempted from LAMal from " + exemption
                .describe() + ": close the exemption before moving to Switzerland");
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
