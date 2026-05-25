package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.infrastructure.cache.CacheConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractQueryAdapter implements ContractQueryPort {

    private final EntityManager entityManager;

    @Override
    @Cacheable(value = CacheConfig.CONTRACT_SUMS_CACHE, key = "#clientId")
    public BigDecimal calculateTotalCostAmountForClient(final UUID clientId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        final Root<ContractJpaEntity> root = query.from(ContractJpaEntity.class);

        final Specification<ContractJpaEntity> spec = ContractCriteriaAdapter.toSpecification(
                ContractSearchCriteria.activeForClient(clientId)
        );

        query.select(cb.coalesce(cb.sum(root.get("costAmount")), BigDecimal.ZERO));
        query.where(spec.toPredicate(root, query, cb));

        final BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }
}

