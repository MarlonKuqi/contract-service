package com.mk.contractservice.domain.contract.exception;

import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class OverlappingContractException extends RuntimeException {

    public OverlappingContractException(final UUID clientId,
            final ContractType type,
            final ContractPeriod existingPeriod) {
        super("Client " + clientId + " already has a " + type + " contract from " + existingPeriod
                .startDate() + " to " + endOf(existingPeriod) + " overlapping this period");
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }

    private static String endOf(final ContractPeriod period) {
        final LocalDateTime endDate = period.endDate();
        return endDate == null ? "no end date" : endDate.toString();
    }
}
