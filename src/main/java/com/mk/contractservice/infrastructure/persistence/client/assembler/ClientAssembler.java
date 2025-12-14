package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.infrastructure.persistence.client.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entity.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entity.PersonJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class ClientAssembler {

    private final PersonAssembler personAssembler;
    private final CompanyAssembler companyAssembler;
    private final EntityManager entityManager;

    public ClientAssembler(final PersonAssembler personAssembler, final CompanyAssembler companyAssembler, final EntityManager entityManager) {
        this.personAssembler = personAssembler;
        this.companyAssembler = companyAssembler;
        this.entityManager = entityManager;
    }

    public ClientJpaEntity toJpaEntity(final Client domain) {
        if (domain.getId() != null) {
            return updateExistingClient(domain);
        }
        return createNewClient(domain);
    }

    public Client toDomain(final ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity personEntity -> personAssembler.toDomain(personEntity);
            case CompanyJpaEntity companyEntity -> companyAssembler.toDomain(companyEntity);
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }

    private ClientJpaEntity updateExistingClient(final Client domain) {
        final ClientJpaEntity existing = entityManager.find(ClientJpaEntity.class, domain.getId());
        existing.setName(domain.getName().value());
        existing.setEmail(domain.getEmail().value());
        existing.setPhone(domain.getPhone().value());
        return existing;
    }

    private ClientJpaEntity createNewClient(final Client domain) {
        return switch (domain) {
            case Person person -> personAssembler.toJpaEntity(person);
            case Company company -> companyAssembler.toJpaEntity(company);
        };
    }
}

