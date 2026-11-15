package com.mk.contractservice.domain.lamal.exception;

import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.lamal.LamalExemption;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class LamalExemptionConflictException extends RuntimeException {

    private LamalExemptionConflictException(final String message) {
        super(message);
    }

    public static LamalExemptionConflictException forLamal(final UUID personId, final LamalExemption exemption) {
        return new LamalExemptionConflictException("Person " + personId + " is exempted from LAMal from " + exemption
                .describe() + ", a LAMal contract cannot cover this period");
    }

    public static LamalExemptionConflictException forExemption(final UUID personId, final ContractPeriod lamalPeriod) {
        final LocalDateTime endDate = lamalPeriod.endDate();
        return new LamalExemptionConflictException("Person " + personId + " has a LAMal contract from " + lamalPeriod
                .startDate() + " to " + (endDate == null ? "no end date" : endDate) + " overlapping this exemption");
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
