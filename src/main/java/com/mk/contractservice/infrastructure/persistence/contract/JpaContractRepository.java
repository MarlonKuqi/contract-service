package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.contract.assembler.ContractAssembler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JpaContractRepository implements ContractRepository {

    ContractJpaRepository contractJpaRepository;
    ContractAssembler assembler;

    @Override
    @CacheEvict(value = "contractSums", key = "#contract.clientId")
    public Contract save(final Contract contract) {
        var entity = assembler.toJpaEntity(contract);
        var savedEntity = contractJpaRepository.save(entity);
        return assembler.toDomain(savedEntity);
    }

    @Override
    public Optional<Contract> findById(final UUID id) {
        return contractJpaRepository.findById(id).map(assembler::toDomain);
    }

    @Override
    public Page<Contract> findActiveByClientIdPageable(final UUID clientId, @Nullable final Optional<LocalDateTime> updatedSince, final Pageable pageable) {
        if (updatedSince.isPresent()) {
            return contractJpaRepository.findActiveByClientIdAndUpdatedAfter(clientId, updatedSince.get(), pageable)
                    .map(assembler::toDomain);
        }
        return contractJpaRepository.findActiveByClientId(clientId, pageable)
                    .map(assembler::toDomain);
    }


    @Override
    @Cacheable(value = "contractSums", key = "#clientId")
    public BigDecimal sumActiveByClientId(final UUID clientId) {
        return contractJpaRepository.sumActiveByClientId(clientId);
    }

    @Override
    @CacheEvict(value = "contractSums", key = "#clientId")
    public int closeAllActiveByClientId(final UUID clientId, final LocalDateTime closureDate) {
        return contractJpaRepository.closeAllActiveByClientId(clientId, closureDate);
    }
}
