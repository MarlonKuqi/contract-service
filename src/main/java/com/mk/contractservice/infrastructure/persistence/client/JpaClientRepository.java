package com.mk.contractservice.infrastructure.persistence.client;


import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.infrastructure.persistence.client.assembler.ClientAssembler;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaClientRepository implements ClientRepository {
    private final ClientJpaRepository jpa;
    private final ClientAssembler assembler;

    public JpaClientRepository(final ClientJpaRepository jpa, final ClientAssembler assembler) {
        this.jpa = jpa;
        this.assembler = assembler;
    }

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
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(final UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByEmail(final ClientEmail clientEmail) {
        return jpa.existsByEmail(clientEmail.value());
    }

    @Override
    public boolean existsByCompanyIdentifier(final CompanyIdentifier companyIdentifier) {
        return jpa.existsByCompanyIdentifier(companyIdentifier.value());
    }
}
