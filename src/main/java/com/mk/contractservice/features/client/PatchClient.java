package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.client.ClientValidationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface PatchClient {

    record Command(
            UUID clientId,
            @Nullable String name,
            @Nullable String email,
            @Nullable String phoneNumber
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
    class Handler implements PatchClient {

        ClientRepository clientRepository;
        ClientService clientService;
        ClientValidationService clientValidationService;

        @Override
        public Client execute(final Command command) {
            Client client = clientService.findClientById(command.clientId());

            boolean hasChanges = false;
            final String newEmail = command.email();
            if (newEmail != null) {
                if (!Objects.equals(client.getEmail().getValue(), newEmail)) {
                    clientValidationService.ensureEmailIsUnique(newEmail);
                }
                client = client.changeEmail(ClientEmail.of(newEmail));
                hasChanges = true;
            }
            final String phoneNumber = command.phoneNumber();
            if (phoneNumber != null) {
                if (!Objects.equals(client.getPhone().getValue(), phoneNumber)) {
                    clientValidationService.ensurePhoneIsUnique(phoneNumber);
                }
                client = client.changePhone(ClientPhoneNumber.of(phoneNumber));
                hasChanges = true;
            }

            if (command.name() != null) {
                client = client.changeName(ClientName.of(command.name()));
                hasChanges = true;
            }

            return hasChanges ? clientRepository.save(client) : client;
        }
    }
}


