package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements PatchClient {

        private final ClientRepository clientRepository;
        private final ClientService clientService;

        @Override
        public Client execute(final Command command) {
            Client client = clientService.findClientById(command.clientId());

            final ClientName name = command.name() == null ? null : ClientName.of(command.name());
            final ClientEmail email = command.email() == null ? null : ClientEmail.of(command.email());
            final ClientPhoneNumber phone = command.phoneNumber() == null ? null : ClientPhoneNumber.of(command.phoneNumber());

            boolean hasChanges = false;
            if (email != null && !client.hasEmail(email)) {
                client = client.changeEmail(email);
                hasChanges = true;
            }
            if (phone != null && !client.hasPhone(phone)) {
                client = client.changePhone(phone);
                hasChanges = true;
            }
            if (name != null && !client.hasName(name)) {
                client = client.changeName(name);
                hasChanges = true;
            }

            return hasChanges ? clientRepository.save(client) : client;
        }
    }
}


