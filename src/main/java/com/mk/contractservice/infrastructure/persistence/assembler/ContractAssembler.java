package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.ContractJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class ContractAssembler {

    private final ClientAssembler clientAssembler;
    private final ClientJpaRepository clientJpaRepository;
    private final EntityManager entityManager;

    public ContractAssembler(final ClientAssembler clientAssembler,
                             final ClientJpaRepository clientJpaRepository,
                             final EntityManager entityManager) {
        this.clientAssembler = clientAssembler;
        this.clientJpaRepository = clientJpaRepository;
        this.entityManager = entityManager;
    }

    public ContractJpaEntity toJpaEntity(Contract domain) {
        if (domain == null) {
            return null;
        }

        ClientJpaEntity clientEntity = clientJpaRepository.findById(domain.getClient().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client must exist - should have been validated in application service"));

        if (domain.getId() != null) {
            ContractJpaEntity existing = entityManager.find(ContractJpaEntity.class, domain.getId());
            if (existing != null) {
                existing.setClient(clientEntity);
                existing.setStartDate(domain.getPeriod().startDate());
                existing.setEndDate(domain.getPeriod().endDate());
                existing.setCostAmount(domain.getCostAmount().value());
                return existing;
            }
        }

        ContractJpaEntity entity = new ContractJpaEntity(
                clientEntity,
                domain.getPeriod().startDate(),
                domain.getPeriod().endDate(),
                domain.getCostAmount().value()
        );

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }

    public Contract toDomain(ContractJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Contract.reconstitute(
                entity.getId(),
                clientAssembler.toDomain(entity.getClient()),
                ContractPeriod.of(entity.getStartDate(), entity.getEndDate()),
                ContractCost.of(entity.getCostAmount())
        );
    }
}
