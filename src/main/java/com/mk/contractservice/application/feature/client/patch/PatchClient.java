package com.mk.contractservice.application.feature.client.patch;

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
import java.util.Optional;
import java.util.UUID;

public interface PatchClient {

    record Command(
            UUID clientId,
            Optional<String> name,
            Optional<String> email,
            Optional<String> phoneNumber
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(name, "Name Optional cannot be null");
            Objects.requireNonNull(email, "Email Optional cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number Optional cannot be null");
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
            final Optional<String> emailOpt = command.email();
            if (emailOpt.isPresent()) {
                final String email = emailOpt.get();
                if (!Objects.equals(client.getEmail().getValue(), email)) {
                    clientValidationService.ensureEmailIsUnique(email);
                }
                client = client.changeEmail(ClientEmail.of(email));
                hasChanges = true;
            }
            final Optional<String> phoneNumberOpt = command.phoneNumber();
            if (phoneNumberOpt.isPresent()) {
                final String phoneNumber = phoneNumberOpt.get();
                if (!Objects.equals(client.getPhone().getValue(), phoneNumber)) {
                    clientValidationService.ensurePhoneIsUnique(phoneNumber);
                }
                client = client.changePhone(ClientPhoneNumber.of(phoneNumber));
                hasChanges = true;
            }
            final Optional<String> nameOpt = command.name();
            if (nameOpt.isPresent()) {
                client = client.changeName(ClientName.of(nameOpt.get()));
                hasChanges = true;
            }
            return hasChanges ? clientRepository.save(client) : client;
        }
    }
}


