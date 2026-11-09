package com.mk.contractservice.domain.contract;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractFixtures {

    public static Contract newContract(
            final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime now
    ) {
        return Contract.of(clientId, ContractPeriod.of(startDate, endDate, now), ContractCost.of(costAmount), now);
    }

    public static Contract existingContract(
            final UUID id,
            final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime lastModified
    ) {
        return Contract.reconstituteFromDatabase(
                id,
                clientId,
                ContractPeriod.reconstituteFromDatabase(startDate, endDate),
                ContractCost.reconstituteFromDatabase(costAmount),
                lastModified
        );
    }
}
