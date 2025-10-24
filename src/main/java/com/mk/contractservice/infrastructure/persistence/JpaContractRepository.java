package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

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
    public List<Contract> findActiveByClientId(final UUID clientId, final OffsetDateTime now, final OffsetDateTime updatedSince) {
        if (updatedSince == null) {
            return springDataRepo.findActiveContracts(clientId, now);
        }
        return springDataRepo.findActiveContractsUpdatedAfter(clientId, now, updatedSince);
    }

    @Override
    public void closeAllActiveByClientId(final UUID clientId, final OffsetDateTime now) {
        springDataRepo.closeAllActiveContracts(clientId, now);
    }

    @Override
    public BigDecimal sumActiveByClientId(final UUID clientId, final OffsetDateTime now) {
        return springDataRepo.sumActiveContracts(clientId, now);
    }
}
