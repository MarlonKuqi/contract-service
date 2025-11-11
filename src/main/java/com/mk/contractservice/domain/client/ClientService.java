package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Person createPerson(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        ensureEmailIsUnique(email);
        return Person.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public Company createCompany(ClientName name, Email email, PhoneNumber phone, CompanyIdentifier companyIdentifier) {
        ensureEmailIsUnique(email);
        ensureCompanyIdentifierIsUnique(companyIdentifier);
        return Company.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }

    public void ensureEmailIsUnique(Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }

    public void ensureCompanyIdentifierIsUnique(CompanyIdentifier identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier.value())) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier.value() + "' already exists",
                    identifier.value()
            );
        }
    }
}
