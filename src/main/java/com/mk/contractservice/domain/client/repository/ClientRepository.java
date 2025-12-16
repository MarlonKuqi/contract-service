package com.mk.contractservice.domain.client.repository;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Optional<Client> findById(final UUID id);

    Client save(Client client);

    void deleteById(final UUID id);

    boolean existsById(final UUID id);

    boolean existsByEmail(final ClientEmail clientEmail);

    boolean existsByCompanyIdentifier(final CompanyIdentifier companyIdentifier);
}
