package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.PersonJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientAssembler {

    private final PersonAssembler personAssembler;
    private final CompanyAssembler companyAssembler;

    public ClientAssembler(final PersonAssembler personAssembler, final CompanyAssembler companyAssembler) {
        this.personAssembler = personAssembler;
        this.companyAssembler = companyAssembler;
    }

    public ClientJpaEntity toJpaEntity(final Client domain) {
        if (domain == null) {
            return null;
        }

        return switch (domain) {
            case Person person -> personAssembler.toJpaEntity(person);
            case Company company -> companyAssembler.toJpaEntity(company);
        };
    }

    public Client toDomain(final ClientJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return switch (entity) {
            case PersonJpaEntity personEntity -> personAssembler.toDomain(personEntity);
            case CompanyJpaEntity companyEntity -> companyAssembler.toDomain(companyEntity);
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }
}

