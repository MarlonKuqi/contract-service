package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.PatchClientUseCase;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PatchClientUseCaseImpl implements PatchClientUseCase {

    ClientRepository clientRepository;
    ClientService clientService;

    @Override
    public Client execute(PatchClientCommand command) {
        Client client = clientService.findClientById(command.clientId());

        if (command.name() == null && command.email() == null && command.phoneNumber() == null) {
            return client;
        }

        if (command.name() != null) {
            client = client.withName(ClientName.of(command.name()));
        }

        if (command.email() != null) {
            client = client.withEmail(ClientEmail.of(command.email()));
        }

        if (command.phoneNumber() != null) {
            client = client.withPhone(ClientPhoneNumber.of(command.phoneNumber()));
        }

        return clientRepository.save(client);
    }
}

