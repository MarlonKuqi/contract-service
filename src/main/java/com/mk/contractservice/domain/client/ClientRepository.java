package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.RepositoryPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RepositoryPort
public interface ClientRepository {
    Optional<Client> findById(final UUID id);

    Client save(final Client client);

    void delete(final UUID id, final LocalDateTime deletedAt);

    boolean existsById(final UUID id);

    boolean existsByCompanyIdentifier(final String companyIdentifier);
}
