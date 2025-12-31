package com.mk.contractservice.application.feature.client.patch;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
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

        @Override
        public Client execute(final Command command) {
            Client client = clientService.findClientById(command.clientId());

            boolean hasChanges = false;
            final Optional<String> nameOpt = command.name();
            if (nameOpt.isPresent()) {
                client = client.changeName(nameOpt.get());
                hasChanges = true;
            }
            final Optional<String> emailOpt = command.email();
            if (emailOpt.isPresent()) {
                client = client.changeEmail(emailOpt.get());
                hasChanges = true;
            }
            final Optional<String> phoneNumberOpt = command.phoneNumber();
            if (phoneNumberOpt.isPresent()) {
                client = client.changePhone(phoneNumberOpt.get());
                hasChanges = true;
            }

            return hasChanges ? clientRepository.save(client) : client;
        }
    }
}


