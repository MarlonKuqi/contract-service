package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyIdentifier;
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

