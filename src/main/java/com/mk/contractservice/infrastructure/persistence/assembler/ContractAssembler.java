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

    public ContractJpaEntity toJpaEntity(final Contract domain) {
        final ClientJpaEntity clientEntity = findClientEntity(domain);

        if (domain.getId() != null) {
            return updateExistingContract(domain, clientEntity);
        }
        return createNewContract(domain, clientEntity);
    }

    public Contract toDomain(final ContractJpaEntity entity) {
        return Contract.reconstitute(
                entity.getId(),
                clientAssembler.toDomain(entity.getClient()),
                ContractPeriod.of(entity.getStartDate(), entity.getEndDate()),
                ContractCost.of(entity.getCostAmount())
        );
    }

    private ClientJpaEntity findClientEntity(final Contract domain) {
        final var clientId = domain.getClient().getId();
        if (clientId == null) {
            throw new IllegalStateException("Client must be persisted before creating a contract (client ID is null)");
        }

        return clientJpaRepository.findById(clientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Client with ID " + clientId + " not found in database"));
    }

    private ContractJpaEntity updateExistingContract(final Contract domain, final ClientJpaEntity clientEntity) {
        final ContractJpaEntity existing = entityManager.find(ContractJpaEntity.class, domain.getId());
        existing.setClient(clientEntity);
        existing.setStartDate(domain.getPeriod().startDate());
        existing.setEndDate(domain.getPeriod().endDate());
        existing.setCostAmount(domain.getCostAmount().value());
        return existing;
    }

    private ContractJpaEntity createNewContract(final Contract domain, final ClientJpaEntity clientEntity) {
        return new ContractJpaEntity(
                clientEntity,
                domain.getPeriod().startDate(),
                domain.getPeriod().endDate(),
                domain.getCostAmount().value()
        );
    }
}
