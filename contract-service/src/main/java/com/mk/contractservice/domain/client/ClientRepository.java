package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.Email;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    Optional<Client> findById(final UUID id);

    Client save(Client client);

    void deleteById(final UUID id);

    boolean existsById(final UUID id);

    boolean existsByEmail(final Email email);
}
