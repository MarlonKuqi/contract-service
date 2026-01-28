package com.mk.contractservice.infrastructure.persistence.contract.assembler;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
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
            return loadExistingContract(domain);
        }
        return createNewContract(domain);
    }

    public Contract toDomain(final ContractJpaEntity entity) {
        return ContractFactory.buildFromDatabase(
                entity.getId(),
                entity.getClientId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCostAmount()
        );
    }

    private ContractJpaEntity loadExistingContract(final Contract domain) {
        final ContractJpaEntity existing = entityManager.find(ContractJpaEntity.class, domain.getId());
        if (existing == null) {
            throw new IllegalStateException("Contract with id " + domain.getId() + " not found in database");
        }
        existing.setClientId(domain.getClientId());
        existing.setStartDate(domain.getPeriod().getStartDate());
        existing.setEndDate(domain.getPeriod().getEndDate());
        existing.setCostAmount(domain.getCostAmount().getValue());
        return existing;
    }

    private ContractJpaEntity createNewContract(final Contract domain) {
        return new ContractJpaEntity(
                domain.getClientId(),
                domain.getPeriod().getStartDate(),
                domain.getPeriod().getEndDate(),
                domain.getCostAmount().getValue()
        );
    }
}
