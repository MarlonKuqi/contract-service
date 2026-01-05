package com.mk.contractservice.domain.client.repository;

import com.mk.contractservice.domain.client.aggregate.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Optional<Client> findById(final UUID id);

    <T extends Client> T save(T client);

    void deleteById(final UUID id);

    boolean existsById(final UUID id);

    boolean existsByEmail(final String email);

    boolean existsByPhoneNumber(final String phoneNumber);

    boolean existsByCompanyIdentifier(final String companyIdentifier);
}
