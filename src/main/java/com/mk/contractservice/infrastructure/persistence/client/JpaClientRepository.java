package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaClientRepository implements ClientRepository {

    private final ClientJpaRepository jpa;
    private final CompanyJpaRepository companyJpa;

    @Override
    public Optional<Client> findById(final UUID id) {
        return jpa.findById(id).map(ClientJpaMapper::toDomain);
    }

    @Override
    public Client save(final Client client) {
        final ClientJpaEntity entity;
        if (client.getId() == null) {
            entity = switch (client) {
                case Person person -> ClientJpaMapper.toNewPersonEntity(person);
                case Company company -> ClientJpaMapper.toNewCompanyEntity(company);
            };
        } else {
            entity = jpa.findById(client.getId()).orElseThrow(
                    () -> new IllegalStateException("Client with id " + client.getId() + " not found in database")
            );
            ClientJpaMapper.mergeIntoExisting(client, entity);
        }
        return ClientJpaMapper.toDomain(jpa.save(entity));
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
        return companyJpa.existsByCompanyIdentifier(companyIdentifier);
    }
}
