package com.mk.contractservice.application.feature.client.update;

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

            final ClientName name = ClientName.of(command.name());
            final ClientEmail email = ClientEmail.of(command.email());
            final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());

            final Client updatedClient = client.withCommonFields(name, email, phone);
            return clientRepository.save(updatedClient);
        }
    }
}

