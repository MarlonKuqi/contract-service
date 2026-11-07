package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.shared.exception.VersionMismatchException;
import com.mk.contractservice.infrastructure.persistence.contract.importjob.ContractImportJobLineJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
        final UUID contractId = contract.id();
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
    public List<Contract> saveAll(final List<Contract> contracts) {
        return contracts.stream()
                .map(this::save)
                .toList();
    }

    @Override
    public Contract getById(final UUID id) {
        return contractJpaRepository.findById(id)
                .map(ContractJpaMapper::toDomain)
                .orElseThrow(() -> new ContractNotFoundException(id));
    }

    @Override
    public void ensureVersion(final UUID contractId, final long expectedVersion) {
        final ContractJpaEntity entity = contractJpaRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
        if (entity.getVersion() != expectedVersion) {
            throw new VersionMismatchException(expectedVersion, entity.getVersion());
        }
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

        return closeAt(update, root, cb, closureDate, wherePredicate);
    }

    @Override
    public int closeAllActiveImportedBy(final UUID importJobId, final LocalDateTime closureDate) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<ContractJpaEntity> update = cb.createCriteriaUpdate(ContractJpaEntity.class);
        final Root<ContractJpaEntity> root = update.from(ContractJpaEntity.class);

        final Subquery<UUID> importedContractIds = update.subquery(UUID.class);
        final Root<ContractImportJobLineJpaEntity> line = importedContractIds.from(ContractImportJobLineJpaEntity.class);
        importedContractIds
                .select(line.get("contractId"))
                .where(cb.equal(line.get("id").get("jobId"), importJobId));

        final Predicate wherePredicate = cb.and(
                ContractSpecifications.isActivePredicate(root, cb, closureDate),
                root.get(ContractSpecifications.FIELD_ID).in(importedContractIds)
        );
        return closeAt(update, root, cb, closureDate, wherePredicate);
    }

    private int closeAt(
            final CriteriaUpdate<ContractJpaEntity> update,
            final Root<ContractJpaEntity> root,
            final CriteriaBuilder cb,
            final LocalDateTime closureDate,
            final Predicate wherePredicate
    ) {
        update.set("endDate", closureDate);
        update.set("lastModified", closureDate);
        update.set(root.<Long>get("version"), cb.sum(root.<Long>get("version"), 1L));
        update.where(wherePredicate);

        return entityManager.createQuery(update).executeUpdate();
    }
}
