package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
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
        return Company.reconstituteFromDatabase(
                entity.getId(),
                ClientName.reconstituteFromDatabase(entity.getName()),
                ClientEmail.reconstituteFromDatabase(entity.getEmail()),
                ClientPhoneNumber.reconstituteFromDatabase(entity.getPhone()),
                CompanyIdentifier.reconstituteFromDatabase(entity.getCompanyIdentifier())
        );
    }
}

