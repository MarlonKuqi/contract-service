package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ClientService {

    ClientRepository clientRepository;

    public ClientService(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Person createPerson(final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        ensureEmailIsUnique(email);
        return Person.of(name, email, phone, birthDate);
    }

    public Company createCompany(final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        ensureEmailIsUnique(email);
        ensureCompanyIdentifierIsUnique(companyIdentifier);
        return Company.of(name, email, phone, companyIdentifier);
    }

    public void ensureEmailIsUnique(final Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }

    public void ensureCompanyIdentifierIsUnique(final CompanyIdentifier identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier.value())) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier.value() + "' already exists",
                    identifier.value()
            );
        }
    }
}
