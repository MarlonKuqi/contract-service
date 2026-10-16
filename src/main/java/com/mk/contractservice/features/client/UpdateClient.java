package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
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

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements UpdateClient {

        private final ClientRepository clientRepository;

        @Override
        public Client execute(final Command command) {
            final Client client = clientRepository.getById(command.clientId());
            final ClientEmail email = ClientEmail.of(command.email());
            final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
            final ClientName name = ClientName.of(command.name());
            final Client updatedClient = client.changeCoreFields(name, email, phone);
            return updatedClient == client ? client : clientRepository.save(updatedClient);
        }
    }
}

