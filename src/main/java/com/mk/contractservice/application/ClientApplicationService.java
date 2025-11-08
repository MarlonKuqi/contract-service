package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ClientApplicationService {

    private final ClientRepository clientRepo;
    private final ContractApplicationService contractService;

    private static final String CLIENT_ALREADY_EXISTS_MSG = "Client already exists";

    public ClientApplicationService(ClientRepository clientRepo, ContractApplicationService contractService) {
        this.clientRepo = clientRepo;
        this.contractService = contractService;
    }

    @Transactional
    public Person createPerson(final String name, final String email, final String phone, final java.time.LocalDate birthDate) {
        if (clientRepo.existsByEmail(email)) {
            throw new ClientAlreadyExistsException(CLIENT_ALREADY_EXISTS_MSG, email);
        }

        final Person person = Person.builder()
                .name(ClientName.of(name))
                .email(Email.of(email))
                .phone(PhoneNumber.of(phone))
                .birthDate(PersonBirthDate.of(birthDate))
                .build();
        return (Person) clientRepo.save(person);
    }

    @Transactional
    public Company createCompany(final String name, final String email, final String phone, final String companyIdentifier) {

        if (clientRepo.existsByEmail(email)) {
            throw new ClientAlreadyExistsException(CLIENT_ALREADY_EXISTS_MSG, email);
        }

        if (clientRepo.existsByCompanyIdentifier(companyIdentifier)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + companyIdentifier + "' already exists",
                    companyIdentifier
            );
        }

        final Company company = Company.builder()
                .name(ClientName.of(name))
                .email(Email.of(email))
                .phone(PhoneNumber.of(phone))
                .companyIdentifier(CompanyIdentifier.of(companyIdentifier))
                .build();
        return (Company) clientRepo.save(company);
    }

    public Optional<Client> findById(final UUID id) {
        return clientRepo.findById(id);
    }

    @Transactional
    public boolean updateCommonFields(final UUID id, final ClientName name, final Email email, final PhoneNumber phone) {
        final Optional<Client> clientOptional = clientRepo.findById(id);
        if (clientOptional.isEmpty()) {
            return false;
        }
        final Client client = clientOptional.get();
        client.updateCommonFields(name, email, phone);
        clientRepo.save(client);
        return true;
    }

    @Transactional
    public boolean deleteClientAndCloseContracts(final UUID id) {
        if (!clientRepo.existsById(id)) return false;
        contractService.closeActiveContractsByClientId(id);
        clientRepo.deleteById(id);
        return true;
    }

}
