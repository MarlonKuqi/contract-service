package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.factory.CompanyFactory;
import com.mk.contractservice.infrastructure.persistence.client.entity.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyAssembler {

    public CompanyJpaEntity toJpaEntity(final Company company) {
        final CompanyJpaEntity entity = CompanyJpaEntity.create(
                company.getName().getValue(),
                company.getEmail().getValue(),
                company.getPhone().getValue(),
                company.getCompanyIdentifier().getValue()
        );

        if (company.getId() != null) {
            entity.setId(company.getId());
        }

        return entity;
    }

    public Company toDomain(final CompanyJpaEntity entity) {
        return CompanyFactory.buildFromDatabase(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCompanyIdentifier()
        );
    }
}

