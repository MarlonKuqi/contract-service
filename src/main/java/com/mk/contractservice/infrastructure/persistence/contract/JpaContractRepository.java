package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractRepository implements ContractRepository {

    private final ContractJpaRepository contractJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Contract save(final Contract contract) {
        final ContractJpaEntity entity;
        final UUID contractId = contract.getId();
        if (contractId == null) {
            entity = ContractJpaMapper.toNewEntity(contract);
        } else {
            entity = contractJpaRepository.findById(contractId).orElseThrow(
                    () -> new IllegalStateException("Contract with id " + contractId + " not found in database")
            );
            ContractJpaMapper.mergeIntoExisting(contract, entity);
        }
        return ContractJpaMapper.toDomain(contractJpaRepository.save(entity));
    }

    @Override
    public Contract getById(final UUID id) {
        return contractJpaRepository.findById(id)
                .map(ContractJpaMapper::toDomain)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    @Override
    public Page<Contract> findByCriteria(final ContractSearchCriteria criteria, final Pageable pageable) {
        return contractJpaRepository.findAll(ContractCriteriaAdapter.toSpecification(criteria), pageable)
                .map(ContractJpaMapper::toDomain);
    }

    @Override
    public List<Contract> findActiveAfter(final LocalDateTime activeAt, @Nullable final UUID afterId, final int limit) {
        Specification<ContractJpaEntity> spec = ContractSpecifications.isActive(activeAt);
        if (afterId != null) {
            spec = spec.and(ContractSpecifications.idGreaterThan(afterId));
        }
        return contractJpaRepository.findBy(spec, query -> query
                        .sortBy(Sort.by(ContractSpecifications.FIELD_ID))
                        .limit(limit)
                        .all())
                .stream()
                .map(ContractJpaMapper::toDomain)
                .toList();
    }

    @Override
    public int closeAllActiveByClientId(final UUID clientId, final LocalDateTime closureDate) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<ContractJpaEntity> update = cb.createCriteriaUpdate(ContractJpaEntity.class);
        final Root<ContractJpaEntity> root = update.from(ContractJpaEntity.class);

        final Predicate wherePredicate = ContractSpecifications.isActiveWithClientIdPredicate(root, cb, clientId, closureDate);

        update.set("endDate", closureDate);
        update.set("lastModified", closureDate);
        update.set(root.<Long>get("version"), cb.sum(root.<Long>get("version"), 1L));
        update.where(wherePredicate);

        return entityManager.createQuery(update).executeUpdate();
    }
}
