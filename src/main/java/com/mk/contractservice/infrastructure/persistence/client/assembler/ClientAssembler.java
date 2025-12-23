package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
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
        existing.setName(domain.getName().value());
        existing.setEmail(domain.getEmail().value());
        existing.setPhone(domain.getPhone().value());
        return existing;
    }

    ClientJpaEntity createNewClient(final Client domain) {
        return switch (domain) {
            case Person person -> personAssembler.toJpaEntity(person);
            case Company company -> companyAssembler.toJpaEntity(company);
        };
    }
}

