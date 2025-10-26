package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientApplicationService {

    private final ClientRepository clientRepo;
    private final ContractRepository contractRepo;

    private static final String CLIENT_ALREADY_EXISTS_MSG = "Client already exists";

    public ClientApplicationService(ClientRepository clientRepo, ContractRepository contractRepo) {
        this.clientRepo = clientRepo;
        this.contractRepo = contractRepo;
    }

    @Transactional
    public Person createPerson(final String name, final String email, final String phone, final java.time.LocalDate birthDate) {
        // Check existence with raw string first (performance optimization)
        if (clientRepo.existsByEmail(email)) {
            throw new ClientAlreadyExistsException(CLIENT_ALREADY_EXISTS_MSG, email);
        }

        final Person person = new Person(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                birthDate
        );
        return (Person) clientRepo.save(person);
    }

    @Transactional
    public Company createCompany(final String name, final String email, final String phone, final String companyIdentifier) {

        if (clientRepo.existsByEmail(email)) {
            throw new ClientAlreadyExistsException(CLIENT_ALREADY_EXISTS_MSG, email);
        }

        final Company company = new Company(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                companyIdentifier
        );
        return (Company) clientRepo.save(company);
    }

    public Optional<Client> findById(final UUID id) {
        return clientRepo.findById(id);
    }

    @Transactional
    public boolean updateCommonFields(final UUID id, final PersonName name, final Email email, final PhoneNumber phone) {
        var opt = clientRepo.findById(id);
        if (opt.isEmpty()) return false;
        var c = opt.get();
        // setters ou recréer un nouvel objet si tu tiens à l’immuabilité stricte
        // (ici, simple) :
        // c.setName(name); c.setEmail(email); c.setPhone(phone);
        // → ajoute ces setters contrôlés dans l’entité si besoin
        return true;
    }

    @Transactional
    public boolean deleteClientAndCloseContracts(final UUID id) {
        if (!clientRepo.existsById(id)) return false;
        final var now = OffsetDateTime.now();
        contractRepo.closeAllActiveByClientId(id, now);
        clientRepo.deleteById(id);
        return true;
    }

}
