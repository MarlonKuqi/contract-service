package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractCriteriaAdapter {

    public static Specification<ContractJpaEntity> toSpecification(final ContractSearchCriteria criteria) {
        Specification<ContractJpaEntity> spec = ContractSpecifications.hasClientId(criteria.clientId());
        if (criteria.activeOnly()) {
            spec = spec.and(ContractSpecifications.isActive());
        }
        if (criteria.updatedSince() == null) {
            return spec;
        }
        return spec.and(ContractSpecifications.updatedAfter(criteria.updatedSince()));

    }
}

