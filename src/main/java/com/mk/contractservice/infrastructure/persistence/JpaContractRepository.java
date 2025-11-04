package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    public List<Contract> findActiveByClientId(final UUID clientId, final LocalDateTime now, final LocalDateTime updatedSince) {
        if (updatedSince == null) {
            return springDataRepo.findActiveContracts(clientId, now);
        }
        return springDataRepo.findActiveContractsUpdatedAfter(clientId, now, updatedSince);
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
