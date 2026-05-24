package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.infrastructure.cache.CacheConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractRepository implements ContractRepository {

    private final ContractJpaRepository contractJpaRepository;
    private final EntityManager entityManager;

    @Override
    @CacheEvict(value = CacheConfig.CONTRACT_SUMS_CACHE, key = "#contract.clientId")
    public Contract save(final Contract contract) {
        final ContractJpaEntity entity;
        if (contract.getId() == null) {
            entity = ContractJpaMapper.toNewEntity(contract);
        } else {
            entity = contractJpaRepository.findById(contract.getId()).orElseThrow(
                    () -> new IllegalStateException("Contract with id " + contract.getId() + " not found in database")
            );
            ContractJpaMapper.mergeIntoExisting(contract, entity);
        }
        return ContractJpaMapper.toDomain(contractJpaRepository.save(entity));
    }

    @Override
    public Optional<Contract> findById(final UUID id) {
        return contractJpaRepository.findById(id).map(ContractJpaMapper::toDomain);
    }

    @Override
    public Page<Contract> findByCriteria(final ContractSearchCriteria criteria, final Pageable pageable) {
        return contractJpaRepository.findAll(ContractCriteriaAdapter.toSpecification(criteria), pageable)
                .map(ContractJpaMapper::toDomain);
    }

    @Override
    @CacheEvict(value = CacheConfig.CONTRACT_SUMS_CACHE, key = "#clientId")
    public int closeAllActiveByClientId(final UUID clientId, final LocalDateTime closureDate) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<ContractJpaEntity> update = cb.createCriteriaUpdate(ContractJpaEntity.class);
        final Root<ContractJpaEntity> root = update.from(ContractJpaEntity.class);

        final Predicate wherePredicate = ContractSpecifications.isActiveWithClientIdPredicate(root, cb, clientId, closureDate);

        // Bulk CriteriaUpdate bypasses Hibernate lifecycle callbacks (@UpdateTimestamp is not triggered).
        // lastModified must be set explicitly here to keep audit fields consistent.
        update.set("endDate", closureDate);
        update.set("lastModified", closureDate);
        update.where(wherePredicate);

        return entityManager.createQuery(update).executeUpdate();
    }
}
