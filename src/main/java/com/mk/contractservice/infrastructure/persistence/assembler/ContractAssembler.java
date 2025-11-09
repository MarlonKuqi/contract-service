package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.ContractJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContractAssembler {

    private final ClientAssembler clientAssembler;
    private final ClientJpaRepository clientJpaRepository;

    public ContractAssembler(ClientAssembler clientAssembler,
                             ClientJpaRepository clientJpaRepository) {
        this.clientAssembler = clientAssembler;
        this.clientJpaRepository = clientJpaRepository;
    }

    public ContractJpaEntity toJpaEntity(Contract domain) {
        if (domain == null) {
            return null;
        }

        ClientJpaEntity clientEntity = clientJpaRepository.findById(domain.getClient().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client must exist - should have been validated in application service"));

        ContractJpaEntity entity = new ContractJpaEntity(
                clientEntity,
                domain.getPeriod().startDate(),
                domain.getPeriod().endDate(),
                domain.getCostAmount().value()
        );

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setLastModified(domain.getLastModified());

        return entity;
    }

    public Contract toDomain(ContractJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Contract.builder()
                .id(entity.getId())
                .client(clientAssembler.toDomain(entity.getClient()))
                .period(ContractPeriod.of(entity.getStartDate(), entity.getEndDate()))
                .costAmount(ContractCost.of(entity.getCostAmount()))
                .build();
    }
}

