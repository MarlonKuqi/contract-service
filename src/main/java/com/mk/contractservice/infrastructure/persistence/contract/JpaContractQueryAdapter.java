package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.ContractDetails;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractQueryAdapter implements ContractQueryPort {

    private final EntityManager entityManager;

    @Override
    public BigDecimal calculateTotalCostAmountForClient(final UUID clientId, final LocalDateTime activeAt) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        final Root<ContractJpaEntity> root = query.from(ContractJpaEntity.class);

        final Specification<ContractJpaEntity> spec = ContractCriteriaAdapter.toSpecification(
                ContractSearchCriteria.activeForClient(clientId, activeAt)
        );

        query.select(cb.coalesce(cb.sum(root.get("costAmount")), BigDecimal.ZERO));
        query.where(spec.toPredicate(root, query, cb));

        final BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    @Override
    public ContractDetails getDetails(final UUID contractId, final LocalDateTime activeAt) {
        final ContractJpaEntity entity = entityManager.find(ContractJpaEntity.class, contractId);
        if (entity == null) {
            throw new ContractNotFoundException(contractId);
        }
        final boolean active = ContractPeriod.reconstituteFromDatabase(entity.getStartDate(), entity.getEndDate())
                .isEffectiveAt(activeAt);
        return new ContractDetails(
                entity.getId(),
                entity.getClientId(),
                entity.getStartDate(),
                entity.getEndDate(),
                active,
                entity.getCostAmount(),
                entity.getVersion()
        );
    }
}

