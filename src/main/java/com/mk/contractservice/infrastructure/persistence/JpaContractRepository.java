package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.assembler.ContractAssembler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaContractRepository implements ContractRepository {

    private final ContractJpaRepository contractJpaRepository;
    private final ContractAssembler assembler;

    public JpaContractRepository(final ContractJpaRepository contractJpaRepository, final ContractAssembler assembler) {
        this.contractJpaRepository = contractJpaRepository;
        this.assembler = assembler;
    }

    @Override
    public Contract save(final Contract contract) {
        if (contract.getId() != null) {
            var entityOpt = contractJpaRepository.findById(contract.getId());
            if (entityOpt.isPresent()) {
                var entity = entityOpt.get();
                entity.setStartDate(contract.getPeriod().startDate());
                entity.setEndDate(contract.getPeriod().endDate());
                entity.setCostAmount(contract.getCostAmount().value());
                var savedEntity = contractJpaRepository.save(entity);
                return assembler.toDomain(contractJpaRepository.findByIdWithClient(savedEntity.getId()).orElseThrow());
            }
        }
        var entity = assembler.toJpaEntity(contract);
        var savedEntity = contractJpaRepository.save(entity);
        return assembler.toDomain(contractJpaRepository.findByIdWithClient(savedEntity.getId()).orElseThrow());
    }

    @Override
    public Optional<Contract> findById(final UUID id) {
        return contractJpaRepository.findByIdWithClient(id).map(assembler::toDomain);
    }

    @Override
    public Page<Contract> findActiveByClientIdPageable(final UUID clientId, final LocalDateTime now, final LocalDateTime updatedSince, final Pageable pageable) {
        if (updatedSince == null) {
            return contractJpaRepository.findActiveContractsPageable(clientId, now, pageable)
                    .map(assembler::toDomain);
        }
        return contractJpaRepository.findActiveContractsUpdatedAfterPageable(clientId, now, updatedSince, pageable)
                .map(assembler::toDomain);
    }


    @Override
    public void closeAllActiveByClientId(final UUID clientId, final LocalDateTime now) {
        contractJpaRepository.closeAllActiveContracts(clientId, now);
    }

    @Override
    public BigDecimal sumActiveByClientId(final UUID clientId, final LocalDateTime now) {
        return contractJpaRepository.sumActiveContracts(clientId, now);
    }
}
