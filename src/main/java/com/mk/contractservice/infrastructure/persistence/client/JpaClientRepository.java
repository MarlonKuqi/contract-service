package com.mk.contractservice.infrastructure.persistence.client;


import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.persistence.client.assembler.ClientAssembler;
import com.mk.contractservice.infrastructure.persistence.client.entity.ClientJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class JpaClientRepository implements ClientRepository {

    ClientJpaRepository jpa;
    ClientAssembler assembler;

    @Override
    public Optional<Client> findById(final UUID id) {
        return jpa.findById(id).map(assembler::toDomain);
    }

    @Override
    public Client save(final Client c) {
        var entity = assembler.toJpaEntity(c);
        var savedEntity = jpa.save(entity);
        return assembler.toDomain(savedEntity);
    }

    @Override
    public void deleteById(final UUID id) {
        final ClientJpaEntity entity = jpa.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + id + " not found"));
        jpa.delete(entity);
    }

    @Override
    public boolean existsById(final UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByEmail(final ClientEmail clientEmail) {
        return jpa.existsByEmail(clientEmail.getValue());
    }

    @Override
    public boolean existsByCompanyIdentifier(final CompanyIdentifier companyIdentifier) {
        return jpa.existsByCompanyIdentifier(companyIdentifier.getValue());
    }
}
