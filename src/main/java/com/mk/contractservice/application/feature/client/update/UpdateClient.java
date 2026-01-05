package com.mk.contractservice.application.feature.client.update;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.service.ClientValidationService;
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
        ClientValidationService clientValidationService;

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

