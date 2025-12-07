package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
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
    public Person createAndPersistPerson(final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        ensureEmailIsUnique(email);
        final Person person = Person.of(name, email, phone, birthDate);
        return (Person) clientRepository.save(person);
    }

    @Transactional
    public Company createAndPersistCompany(final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        ensureEmailIsUnique(email);
        ensureCompanyIdentifierIsUnique(companyIdentifier);
        final Company company = Company.of(name, email, phone, companyIdentifier);
        return (Company) clientRepository.save(company);
    }

    public void ensureEmailIsUnique(final Email email) {
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
    public Client updateAndPersistCommonFields(final UUID clientId, final ClientName name, final Email email, final PhoneNumber phone) {
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));

        final Client updatedClient = switch (client) {
            case Person p -> p.withCommonFields(name, email, phone);
            case Company c -> c.withCommonFields(name, email, phone);
        };

        return clientRepository.save(updatedClient);
    }

    @Transactional
    public Client patchAndPersistClient(
            final UUID clientId,
            final @Nullable ClientName name,
            final @Nullable Email email,
            final @Nullable PhoneNumber phone
    ) {
        final Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));

        if (name == null && email == null && phone == null) {
            return client;
        }

        final Client patchedClient = client.updatePartial(name, email, phone);
        return clientRepository.save(patchedClient);
    }
}
