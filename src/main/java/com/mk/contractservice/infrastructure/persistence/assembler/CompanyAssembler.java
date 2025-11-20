package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.infrastructure.persistence.entity.CompanyJpaEntity;
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
        if (entity == null) {
            return null;
        }

        return Company.reconstitute(
                entity.getId(),
                ClientName.of(entity.getName()),
                Email.of(entity.getEmail()),
                PhoneNumber.of(entity.getPhone()),
                CompanyIdentifier.of(entity.getCompanyIdentifier())
        );
    }
}

