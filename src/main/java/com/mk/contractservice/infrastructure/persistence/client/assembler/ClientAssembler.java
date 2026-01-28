package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.infrastructure.persistence.client.entity.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entity.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entity.PersonJpaEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ClientAssembler {

    PersonAssembler personAssembler;
    CompanyAssembler companyAssembler;
    EntityManager entityManager;


    public ClientJpaEntity toJpaEntity(final Client domain) {
        if (domain.getId() != null) {
            return loadExistingClient(domain);
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

    ClientJpaEntity loadExistingClient(final Client domain) {
        final ClientJpaEntity existing = entityManager.find(ClientJpaEntity.class, domain.getId());
        if (existing == null) {
            throw new IllegalStateException("Client with id " + domain.getId() + " not found in database");
        }
        existing.setName(domain.getName().getValue());
        existing.setEmail(domain.getEmail().getValue());
        existing.setPhone(domain.getPhone().getValue());
        return existing;
    }

    ClientJpaEntity createNewClient(final Client domain) {
        return switch (domain) {
            case Person person -> personAssembler.toJpaEntity(person);
            case Company company -> companyAssembler.toJpaEntity(company);
        };
    }
}

