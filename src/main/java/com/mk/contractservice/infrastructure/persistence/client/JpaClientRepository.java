package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.domain.shared.exception.VersionMismatchException;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaClientRepository implements ClientRepository {

    private final ClientJpaRepository jpa;
    private final CompanyJpaRepository companyJpa;

    @Override
    public Client getById(final UUID id) {
        return jpa.findById(id)
                .map(ClientJpaMapper::toDomain)
                .orElseThrow(() -> ClientNotFoundException.forId(id));
    }

    @Override
    public Client save(final Client client) {
        final ClientJpaEntity entity;
        final UUID clientId = client.getId();
        if (clientId == null) {
            entity = switch (client) {
                case Person person -> ClientJpaMapper.toNewPersonEntity(person);
                case Company company -> ClientJpaMapper.toNewCompanyEntity(company);
            };
        } else {
            entity = jpa.findById(clientId).orElseThrow(
                    () -> new IllegalStateException("Client with id " + clientId + " not found in database")
            );
            ClientJpaMapper.mergeIntoExisting(client, entity);
        }
        return ClientJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public void delete(final UUID id, final LocalDateTime deletedAt) {
        final ClientJpaEntity entity = jpa.findById(id).orElseThrow(
                () -> new IllegalStateException("Client with id " + id + " not found in database")
        );
        entity.setDeletedAt(deletedAt);
    }

    @Override
    public void ensureExists(final UUID id) {
        if (!jpa.existsById(id)) {
            throw ClientNotFoundException.forId(id);
        }
    }

    @Override
    public void ensureVersion(final UUID id, final long expectedVersion) {
        final ClientJpaEntity entity = jpa.findById(id).orElseThrow(() -> ClientNotFoundException.forId(id));
        if (entity.getVersion() != expectedVersion) {
            throw new VersionMismatchException(expectedVersion, entity.getVersion());
        }
    }

    @Override
    public boolean existsByCompanyIdentifier(final String companyIdentifier) {
        return companyJpa.existsByCompanyIdentifierIncludingDeleted(companyIdentifier);
    }
}
