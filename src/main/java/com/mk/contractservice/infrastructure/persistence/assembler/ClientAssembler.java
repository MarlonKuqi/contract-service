package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.PersonJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientAssembler {

    public ClientJpaEntity toJpaEntity(Client domain) {
        if (domain == null) {
            return null;
        }

        ClientJpaEntity entity = switch (domain) {
            case Person person -> PersonJpaEntity.create(
                    person.getName().value(),
                    person.getEmail().value(),
                    person.getPhone().value(),
                    person.getBirthDate().value()
            );
            case Company company -> CompanyJpaEntity.create(
                    company.getName().value(),
                    company.getEmail().value(),
                    company.getPhone().value(),
                    company.getCompanyIdentifier().value()
            );
        };

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        return entity;
    }

    public Client toDomain(ClientJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return switch (entity) {
            case PersonJpaEntity personEntity -> Person.builder()
                    .id(personEntity.getId())
                    .name(ClientName.of(personEntity.getName()))
                    .email(Email.of(personEntity.getEmail()))
                    .phone(PhoneNumber.of(personEntity.getPhone()))
                    .birthDate(PersonBirthDate.of(personEntity.getBirthDate()))
                    .build();
            case CompanyJpaEntity companyEntity -> Company.builder()
                    .id(companyEntity.getId())
                    .name(ClientName.of(companyEntity.getName()))
                    .email(Email.of(companyEntity.getEmail()))
                    .phone(PhoneNumber.of(companyEntity.getPhone()))
                    .companyIdentifier(CompanyIdentifier.of(companyEntity.getCompanyIdentifier()))
                    .build();
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }
}

