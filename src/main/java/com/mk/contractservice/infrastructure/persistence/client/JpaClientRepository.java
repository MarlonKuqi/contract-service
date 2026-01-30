package com.mk.contractservice.infrastructure.persistence.client;


import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.infrastructure.persistence.client.assemblers.ClientAssembler;
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
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(final UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByEmail(final String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(final String phoneNumber) {
        return jpa.existsByPhone(phoneNumber);
    }

    @Override
    public boolean existsByCompanyIdentifier(final String companyIdentifier) {
        return jpa.existsByCompanyIdentifier(companyIdentifier);
    }
}
