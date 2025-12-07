package com.mk.contractservice.application.service;

import com.mk.contractservice.application.dto.ClientDto;
import com.mk.contractservice.application.dto.CompanyDto;
import com.mk.contractservice.application.dto.PersonDto;
import com.mk.contractservice.application.mapper.ClientMapper;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ClientApplicationService {

    private final ClientRepository clientRepo;
    private final ContractApplicationService contractService;
    private final ClientService clientService;
    private final ClientMapper mapper;

    public ClientApplicationService(
            final ClientRepository clientRepo,
            final ContractApplicationService contractService,
            final ClientService clientService,
            final ClientMapper mapper) {
        this.clientRepo = clientRepo;
        this.contractService = contractService;
        this.clientService = clientService;
        this.mapper = mapper;
    }

    @Transactional
    public PersonDto createPerson(final String name, final String email, final String phone, final LocalDate birthDate) {
        final Person person = clientService.createAndPersistPerson(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                PersonBirthDate.of(birthDate)
        );
        return mapper.toPersonDto(person);
    }

    @Transactional
    public CompanyDto createCompany(final String name, final String email, final String phone, final String companyIdentifier) {
        final Company company = clientService.createAndPersistCompany(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                CompanyIdentifier.of(companyIdentifier)
        );
        return mapper.toCompanyDto(company);
    }

    @Transactional(readOnly = true)
    public ClientDto getClientById(final UUID id) {
        final Client client = findClientOrThrow(id);
        return mapper.toDto(client);
    }

    @Transactional
    public ClientDto updateCommonFields(final UUID id, final String name, final String email, final String phone) {
        final Client client = clientService.updateAndPersistCommonFields(
                id,
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone)
        );
        return mapper.toDto(client);
    }

    @Transactional
    public ClientDto patchClient(
            final UUID id,
            final @Nullable String name,
            final @Nullable String email,
            final @Nullable String phone
    ) {
        final Client client = clientService.patchAndPersistClient(
                id,
                name == null ? null : ClientName.of(name),
                email == null ? null : Email.of(email),
                phone == null ? null : PhoneNumber.of(phone)
        );
        return mapper.toDto(client);
    }

    @Transactional
    public void deleteClientAndCloseContracts(final UUID id) {
        if (!clientRepo.existsById(id)) {
            throw clientNotFound(id);
        }
        contractService.closeActiveContractsByClientId(id);
        clientRepo.deleteById(id);
    }

    private Client findClientOrThrow(final UUID id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> clientNotFound(id));
    }

    private static ClientNotFoundException clientNotFound(final UUID id) {
        return new ClientNotFoundException("Client with ID " + id + " not found");
    }

}
