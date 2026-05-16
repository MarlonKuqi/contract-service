package com.mk.contractservice.infrastructure.persistence.contract;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractSpecifications {

    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String FIELD_LAST_MODIFIED = "lastModified";

    public static Predicate isActivePredicate(final Root<ContractJpaEntity> root,
                                              final CriteriaBuilder cb,
                                              final LocalDateTime referenceDate) {
        return cb.or(
                cb.isNull(root.get(FIELD_END_DATE)),
                cb.greaterThan(root.get(FIELD_END_DATE), referenceDate)
        );
    }

    public static Predicate hasClientIdPredicate(final Root<ContractJpaEntity> root,
                                                 final CriteriaBuilder cb,
                                                 final UUID clientId) {
        return cb.equal(root.get(FIELD_CLIENT_ID), clientId);
    }

    public static Predicate isActiveWithClientIdPredicate(final Root<ContractJpaEntity> root,
                                                          final CriteriaBuilder cb,
                                                          final UUID clientId,
                                                          final LocalDateTime referenceDate) {
        return cb.and(
                isActivePredicate(root, cb, referenceDate),
                hasClientIdPredicate(root, cb, clientId)
        );
    }

    public static Predicate updatedAfterPredicate(final Root<ContractJpaEntity> root,
                                                  final CriteriaBuilder cb,
                                                  final LocalDateTime updatedSince) {
        return cb.greaterThanOrEqualTo(root.get(FIELD_LAST_MODIFIED), updatedSince);
    }

    public static Specification<ContractJpaEntity> isActive() {
        return isActive(LocalDateTime.now());
    }

    public static Specification<ContractJpaEntity> isActive(final LocalDateTime referenceDate) {
        return (contract, query, cb) -> isActivePredicate(contract, cb, referenceDate);
    }

    public static Specification<ContractJpaEntity> hasClientId(final UUID clientId) {
        return (contract, query, cb) -> hasClientIdPredicate(contract, cb, clientId);
    }

    public static Specification<ContractJpaEntity> isActiveWithClientId(final UUID clientId) {
        return (contract, query, cb) -> isActiveWithClientIdPredicate(contract, cb, clientId, LocalDateTime.now());
    }

    public static Specification<ContractJpaEntity> updatedAfter(final LocalDateTime updatedSince) {
        return (contract, query, cb) -> updatedAfterPredicate(contract, cb, updatedSince);
    }
}
