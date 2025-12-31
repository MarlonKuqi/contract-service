package com.mk.contractservice.application.feature.client.update;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface UpdateClient {

    record Command(
            UUID clientId,
            String name,
            String email,
            String phoneNumber
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        }
    }

    Client execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements UpdateClient {

        ClientRepository clientRepository;
        ClientService clientService;

        @Override
        public Client execute(final Command command) {
            final Client client = clientService.findClientById(command.clientId());

            final Client updatedClient = client.changeCoreFields(
                    command.name(),
                    command.email(),
                    command.phoneNumber()
            );

            return clientRepository.save(updatedClient);
        }
    }
}

