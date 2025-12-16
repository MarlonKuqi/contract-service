package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.infrastructure.persistence.client.entity.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyAssembler {


    public CompanyJpaEntity toJpaEntity(final Company company) {
        final CompanyJpaEntity entity = CompanyJpaEntity.create(
                company.getName().value(),
                company.getEmail().value(),
                company.getPhone().value(),
                company.getCompanyIdentifier().value()
        );

        if (company.getId() != null) {
            entity.setId(company.getId());
        }

        return entity;
    }

    public Company toDomain(final CompanyJpaEntity entity) {
        return Company.reconstitute(
                entity.getId(),
                ClientName.of(entity.getName()),
                ClientEmail.of(entity.getEmail()),
                ClientPhoneNumber.of(entity.getPhone()),
                CompanyIdentifier.of(entity.getCompanyIdentifier())
        );
    }
}

