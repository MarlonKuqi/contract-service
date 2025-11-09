package com.mk.contractservice.infrastructure.persistence;


import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.infrastructure.persistence.assembler.ClientAssembler;
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
    public boolean existsByEmail(final Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public boolean existsByEmail(final String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByCompanyIdentifier(final String companyIdentifier) {
        return jpa.existsByCompanyIdentifier(companyIdentifier);
    }
}
