package com.mk.contractservice.domain.contract.factory;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ContractFactory {

    public static Contract createFromCommand(
            final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount
    ) {
        final ContractPeriod period = ContractPeriod.of(startDate, endDate);
        final ContractCost cost = ContractCost.of(costAmount);
        return Contract.of(clientId, period, cost);
    }

    public static Contract buildFromDatabase(
            final UUID id,
            final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal costAmount
    ) {
        final ContractPeriod period = ContractPeriod.reconstituteFromDatabase(startDate, endDate);
        final ContractCost cost = ContractCost.reconstituteFromDatabase(costAmount);

        return Contract.reconstituteFromDatabase(id, clientId, period, cost);
    }
}

