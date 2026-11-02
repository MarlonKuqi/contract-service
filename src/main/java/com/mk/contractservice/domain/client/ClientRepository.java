package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.RepositoryPort;

import java.time.LocalDateTime;
import java.util.UUID;

@RepositoryPort
public interface ClientRepository {
    Client getById(final UUID id);

    Client save(final Client client);

    void delete(final UUID id, final LocalDateTime deletedAt);

    void ensureExists(final UUID id);

    void ensureVersion(final UUID id, final long expectedVersion);

    boolean existsByCompanyIdentifier(final String companyIdentifier);
}
