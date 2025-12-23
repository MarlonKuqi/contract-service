package com.mk.contractservice.infrastructure.persistence.contract.assembler;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.infrastructure.persistence.contract.entity.ContractJpaEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ContractAssembler {

    EntityManager entityManager;

    public ContractJpaEntity toJpaEntity(final Contract domain) {
        if (domain.getId() != null) {
            return updateExistingContract(domain);
        }
        return createNewContract(domain);
    }

    public Contract toDomain(final ContractJpaEntity entity) {
        return Contract.reconstitute(
                entity.getId(),
                entity.getClientId(),
                ContractPeriod.of(entity.getStartDate(), entity.getEndDate()),
                ContractCost.of(entity.getCostAmount())
        );
    }

    private ContractJpaEntity updateExistingContract(final Contract domain) {
        final ContractJpaEntity existing = entityManager.find(ContractJpaEntity.class, domain.getId());
        existing.setClientId(domain.getClientId());
        existing.setStartDate(domain.getPeriod().startDate());
        existing.setEndDate(domain.getPeriod().endDate());
        existing.setCostAmount(domain.getCostAmount().value());
        return existing;
    }

    private ContractJpaEntity createNewContract(final Contract domain) {
        return new ContractJpaEntity(
                domain.getClientId(),
                domain.getPeriod().startDate(),
                domain.getPeriod().endDate(),
                domain.getCostAmount().value()
        );
    }
}
