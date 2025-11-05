package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaContractRepository implements ContractRepository {

    private final ContractJpaRepository springDataRepo;

    public JpaContractRepository(final ContractJpaRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Contract save(final Contract contract) {
        return springDataRepo.save(contract);
    }

    @Override
    public Optional<Contract> findById(final UUID id) {
        return springDataRepo.findById(id);
    }

    @Override
    public Page<Contract> findActiveByClientIdPageable(final UUID clientId, final LocalDateTime now, final LocalDateTime updatedSince, final Pageable pageable) {
        if (updatedSince == null) {
            return springDataRepo.findActiveContractsPageable(clientId, now, pageable);
        }
        return springDataRepo.findActiveContractsUpdatedAfterPageable(clientId, now, updatedSince, pageable);
    }


    @Override
    public void closeAllActiveByClientId(final UUID clientId, final LocalDateTime now) {
        springDataRepo.closeAllActiveContracts(clientId, now);
    }

    @Override
    public BigDecimal sumActiveByClientId(final UUID clientId, final LocalDateTime now) {
        return springDataRepo.sumActiveContracts(clientId, now);
    }
}
