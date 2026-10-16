package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.RepositoryPort;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;

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

    default Client getById(final UUID id) {
        return findById(id).orElseThrow(() -> ClientNotFoundException.forId(id));
    }

    default void ensureExists(final UUID id) {
        if (!existsById(id)) {
            throw ClientNotFoundException.forId(id);
        }
    }
}
