package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
    EntityManager entityManager;

    @Override
    @CacheEvict(value = "contractSums", key = "#contract.clientId")
    public Contract save(final Contract contract) {
        final var entity = assembler.toJpaEntity(contract);
        final var savedEntity = contractJpaRepository.save(entity);
        return assembler.toDomain(savedEntity);
    }

    @Override
    public Optional<Contract> findById(final UUID id) {
        return contractJpaRepository.findById(id).map(assembler::toDomain);
    }

    @Override
    public Page<Contract> findActiveByClientIdPageable(final UUID clientId, @Nullable final LocalDateTime updatedSince, final Pageable pageable) {
        final var specification = ContractSpecifications.isActiveWithClientId(clientId);
        if (updatedSince == null) {
            return contractJpaRepository.findAll(specification, pageable).map(assembler::toDomain);
        }
        return contractJpaRepository.findAll(specification.and(ContractSpecifications.updatedAfter(updatedSince)), pageable).map(assembler::toDomain);
    }


    @Override
    @Cacheable(value = "contractSums", key = "#clientId")
    public BigDecimal calculateTotalCostAmountForClient(final UUID clientId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        final Root<ContractJpaEntity> root = query.from(ContractJpaEntity.class);

        final var specification = ContractSpecifications.isActiveWithClientId(clientId);

        query.select(cb.coalesce(cb.sum(root.get("costAmount")), BigDecimal.ZERO));
        query.where(specification.toPredicate(root, query, cb));

        final BigDecimal totalCostAmount = entityManager.createQuery(query).getSingleResult();
        return totalCostAmount == null ? BigDecimal.ZERO : totalCostAmount;
    }

    @Override
    @CacheEvict(value = "contractSums", key = "#clientId")
    public int closeAllActiveByClientId(final UUID clientId, final LocalDateTime closureDate) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<ContractJpaEntity> update = cb.createCriteriaUpdate(ContractJpaEntity.class);
        final Root<ContractJpaEntity> root = update.from(ContractJpaEntity.class);

        final Predicate wherePredicate = cb.and(
                ContractSpecifications.isActivePredicate(root, cb, closureDate),
                ContractSpecifications.hasClientIdPredicate(root, cb, clientId)
        );

        update.set("endDate", closureDate);
        update.set("lastModified", closureDate);
        update.where(wherePredicate);

        return entityManager.createQuery(update).executeUpdate();
    }
}
