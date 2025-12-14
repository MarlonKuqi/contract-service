package com.mk.contractservice.domain.client;

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
