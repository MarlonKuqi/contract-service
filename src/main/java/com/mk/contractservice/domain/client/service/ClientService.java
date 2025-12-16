package com.mk.contractservice.domain.client.service;

import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.valueobject.*;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientService {

    final ClientRepository clientRepository;

    public ClientService(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Person createAndPersistPerson(final ClientName name, final ClientEmail clientEmail, final ClientPhoneNumber phone, final PersonBirthDate birthDate) {
        ensureEmailIsUnique(clientEmail);
        final Person person = Person.of(name, clientEmail, phone, birthDate);
        return (Person) clientRepository.save(person);
    }

    @Transactional
    public Company createAndPersistCompany(final ClientName name, final ClientEmail email, final ClientPhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        ensureEmailIsUnique(email);
        ensureCompanyIdentifierIsUnique(companyIdentifier);
        final Company company = Company.of(name, email, phone, companyIdentifier);
        return (Company) clientRepository.save(company);
    }

    public void ensureEmailIsUnique(final ClientEmail email) {
        if (clientRepository.existsByEmail(email)) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }

    public void ensureCompanyIdentifierIsUnique(final CompanyIdentifier identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier.value() + "' already exists",
                    identifier.value()
            );
        }
    }

    @Transactional
    public Client updateAndPersistCommonFields(final UUID clientId, final ClientName name, final ClientEmail clientEmail, final ClientPhoneNumber phone) {
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));

        final Client updatedClient = switch (client) {
            case Person p -> p.withCommonFields(name, clientEmail, phone);
            case Company c -> c.withCommonFields(name, clientEmail, phone);
        };

        return clientRepository.save(updatedClient);
    }

    @Transactional
    public Client patchAndPersistClient(
            final UUID clientId,
            final @Nullable ClientName name,
            final @Nullable ClientEmail clientEmail,
            final @Nullable ClientPhoneNumber phone
    ) {
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));

        if (name == null && clientEmail == null && phone == null) {
            return client;
        }

        final Client patchedClient = client.updatePartial(name, clientEmail, phone);
        return clientRepository.save(patchedClient);
    }
}
