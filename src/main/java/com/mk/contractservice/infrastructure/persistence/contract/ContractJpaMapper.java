package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractCoverage;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.LamalFranchise;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractJpaMapper {

    public static Contract toDomain(final ContractJpaEntity entity) {
        return Contract.reconstituteFromDatabase(
                entity.getId(),
                entity.getClientId(),
                ContractCoverage.reconstituteFromDatabase(entity.getType(), entity.getFranchise()),
                ContractPeriod.reconstituteFromDatabase(entity.getStartDate(), entity.getEndDate()),
                ContractCost.reconstituteFromDatabase(entity.getCostAmount()),
                entity.getLastModified()
        );
    }

    public static ContractJpaEntity toNewEntity(final Contract domain) {
        return new ContractJpaEntity(
                domain.clientId(),
                domain.type(),
                domain.franchise().map(LamalFranchise::value).orElse(null),
                domain.period().startDate(),
                domain.period().endDate(),
                domain.costAmount().value(),
                domain.lastModified()
        );
    }

    public static void mergeIntoExisting(final Contract domain, final ContractJpaEntity existing) {
        existing.setClientId(domain.clientId());
        existing.setStartDate(domain.period().startDate());
        existing.setEndDate(domain.period().endDate());
        existing.setCostAmount(domain.costAmount().value());
        existing.setLastModified(domain.lastModified());
    }
}
