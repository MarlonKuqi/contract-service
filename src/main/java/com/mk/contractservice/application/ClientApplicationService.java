package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ClientApplicationService {

    private final ClientRepository clientRepo;
    private final ContractApplicationService contractService;
    private final ClientService clientService;

    public ClientApplicationService(
            ClientRepository clientRepo,
            ContractApplicationService contractService,
            ClientService clientService) {
        this.clientRepo = clientRepo;
        this.contractService = contractService;
        this.clientService = clientService;
    }

    @Transactional
    public Person createPerson(final String name, final String email, final String phone, final LocalDate birthDate) {
        final Person person = clientService.createPerson(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                PersonBirthDate.of(birthDate)
        );
        return (Person) clientRepo.save(person);
    }

    @Transactional
    public Company createCompany(final String name, final String email, final String phone, final String companyIdentifier) {
        final Company company = clientService.createCompany(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                CompanyIdentifier.of(companyIdentifier)
        );
        return (Company) clientRepo.save(company);
    }

    @Transactional(readOnly = true)
    public Client getClientById(final UUID id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + id + " not found"));
    }

    @Transactional
    public Client updateCommonFields(final UUID id, final ClientName name, final Email email, final PhoneNumber phone) {
        final Client client = getClientById(id);

        final Client updatedClient = switch (client) {
            case Person p -> p.withCommonFields(name, email, phone);
            case Company c -> c.withCommonFields(name, email, phone);
        };

        return clientRepo.save(updatedClient);
    }

    @Transactional
    public Client patchClient(final UUID id, final ClientName name, final Email email, final PhoneNumber phone) {
        Client client = getClientById(id);

        if (name == null && email == null && phone == null) {
            return client;
        }

        Client patchedClient = client.updatePartial(name, email, phone);
        return clientRepo.save(patchedClient);
    }

    @Transactional
    public void deleteClientAndCloseContracts(final UUID id) {
        if (!clientRepo.existsById(id)) {
            throw new ClientNotFoundException("Client with ID " + id + " not found");
        }
        contractService.closeActiveContractsByClientId(id);
        clientRepo.deleteById(id);
    }

}
