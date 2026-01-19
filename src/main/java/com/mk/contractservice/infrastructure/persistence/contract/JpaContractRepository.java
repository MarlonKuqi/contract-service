package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.contract.assembler.ContractAssembler;
import com.mk.contractservice.infrastructure.persistence.contract.entity.ContractJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
    EntityManager entityManager;

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
    public Page<Contract> findActiveByClientIdPageable(final UUID clientId, final Optional<LocalDateTime> updatedSince, final Pageable pageable) {
        var specification = ContractSpecifications.builder()
                .active()
                .withClientId(clientId)
                .updatedAfter(updatedSince.orElse(null))
                .build();

        return contractJpaRepository.findAll(specification, pageable)
                .map(assembler::toDomain);
    }


    @Override
    @Cacheable(value = "contractSums", key = "#clientId")
    public BigDecimal sumActiveByClientId(final UUID clientId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<ContractJpaEntity> root = query.from(ContractJpaEntity.class);

        var specification = ContractSpecifications.builder()
                .active()
                .withClientId(clientId)
                .build();

        query.select(cb.coalesce(cb.sum(root.get("costAmount")), BigDecimal.ZERO))
                .where(specification.toPredicate(root, query, cb));

        BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    @CacheEvict(value = "contractSums", key = "#clientId")
    public int closeAllActiveByClientId(final UUID clientId, final LocalDateTime closureDate) {
        return contractJpaRepository.closeAllActiveByClientId(clientId, closureDate);
    }
}
