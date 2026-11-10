package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
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
        return newContract(ClientFixtures.existingPerson(clientId),
                ContractType.LAMAL,
                startDate,
                endDate,
                costAmount,
                now);
    }

    public static Contract newContract(
            final Client client,
            final ContractType type,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime now
    ) {
        return Contract.of(client.id(),
                client.kind(),
                type,
                ContractPeriod.of(startDate, endDate, now),
                ContractCost.of(costAmount),
                now);
    }

    public static Contract existingContract(
            final UUID id,
            final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime lastModified
    ) {
        return existingContract(id, clientId, ContractType.LAMAL, startDate, endDate, costAmount, lastModified);
    }

    public static Contract existingContract(
            final UUID id,
            final UUID clientId,
            final ContractType type,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime lastModified
    ) {
        return Contract.reconstituteFromDatabase(
                id,
                clientId,
                type,
                ContractPeriod.reconstituteFromDatabase(startDate, endDate),
                ContractCost.reconstituteFromDatabase(costAmount),
                lastModified
        );
    }
}
