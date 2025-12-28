package com.mk.contractservice.application.feature.client.patch;

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

import java.util.Objects;
import java.util.UUID;

public sealed interface PatchClient permits PatchClient.Handler {

    record Command(
            UUID clientId,
            String name,
            String email,
            String phoneNumber
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    Client execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements PatchClient {

        ClientRepository clientRepository;
        ClientService clientService;

        @Override
        public Client execute(final Command command) {
            Client client = clientService.findClientById(command.clientId());

            boolean hasChanges = false;

            if (command.name() != null) {
                final ClientName name = ClientName.of(command.name());
                client = client.withName(name);
                hasChanges = true;
            }

            if (command.email() != null) {
                final ClientEmail email = ClientEmail.of(command.email());
                client = client.withEmail(email);
                hasChanges = true;
            }

            if (command.phoneNumber() != null) {
                final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
                client = client.withPhone(phone);
                hasChanges = true;
            }

            return hasChanges ? clientRepository.save(client) : client;
        }
    }
}


