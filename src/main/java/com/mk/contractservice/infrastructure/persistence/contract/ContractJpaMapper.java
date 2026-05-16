package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractJpaMapper {

    public static Contract toDomain(final ContractJpaEntity entity) {
        return ContractFactory.buildFromDatabase(
                entity.getId(),
                entity.getClientId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCostAmount()
        );
    }

    public static ContractJpaEntity toNewEntity(final Contract domain) {
        return new ContractJpaEntity(
                domain.getClientId(),
                domain.getPeriod().getStartDate(),
                domain.getPeriod().getEndDate(),
                domain.getCostAmount().getValue()
        );
    }

    public static void mergeIntoExisting(final Contract domain, final ContractJpaEntity existing) {
        existing.setClientId(domain.getClientId());
        existing.setStartDate(domain.getPeriod().getStartDate());
        existing.setEndDate(domain.getPeriod().getEndDate());
        existing.setCostAmount(domain.getCostAmount().getValue());
    }
}

