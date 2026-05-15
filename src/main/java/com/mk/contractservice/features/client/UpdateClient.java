package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.client.ClientValidationService;
import lombok.RequiredArgsConstructor;
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
    class Handler implements UpdateClient {

        private final ClientRepository clientRepository;
        private final ClientService clientService;
        private final ClientValidationService clientValidationService;

        @Override
        public Client execute(final Command command) {
            final Client client = clientService.findClientById(command.clientId());
            final String commandEmail = command.email();
            if (!Objects.equals(client.getEmail().getValue(), commandEmail)) {
                clientValidationService.ensureEmailIsUnique(commandEmail);
            }
            final String commandPhoneNumber = command.phoneNumber();
            if (!Objects.equals(client.getPhone().getValue(), commandPhoneNumber)) {
                clientValidationService.ensurePhoneIsUnique(commandPhoneNumber);
            }
            final Client updatedClient = client.changeCoreFields(
                    ClientName.of(command.name()),
                    ClientEmail.of(commandEmail),
                    ClientPhoneNumber.of(commandPhoneNumber)
            );

            return clientRepository.save(updatedClient);
        }
    }
}

