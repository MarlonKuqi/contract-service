package com.mk.contractservice.infrastructure.persistence;


import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import java.util.Optional;
import java.util.UUID;

import com.mk.contractservice.domain.valueobject.Email;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClientRepository implements ClientRepository {
    private final ClientJpaRepository jpa;

    public JpaClientRepository(final ClientJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Client> findById(final UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Client save(final Client c) {
        return jpa.save(c);
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
        return jpa.existsByEmail_Value(email.value());
    }

    @Override
    public boolean existsByEmail(final String email) {
        return jpa.existsByEmail_Value(email);
    }
}
