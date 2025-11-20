package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.entity.PersonJpaEntity;
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
        if (domain == null) {
            return null;
        }
        if (domain.getId() != null) {
            ClientJpaEntity existing = entityManager.find(ClientJpaEntity.class, domain.getId());
            if (existing != null) {
                existing.setName(domain.getName().value());
                existing.setEmail(domain.getEmail().value());
                existing.setPhone(domain.getPhone().value());
                return existing;
            }
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

