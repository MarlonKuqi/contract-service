package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface PatchClient {

    record Command(
            UUID clientId,
            long expectedVersion,
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

        @Override
        @Retryable(
                includes = OptimisticLockingFailureException.class,
                maxRetries = 3,
                delay = 20,
                jitter = 10
        )
        public Client execute(final Command command) {
            final Client client = clientRepository.getById(command.clientId());
            clientRepository.ensureVersion(command.clientId(), command.expectedVersion());

            final ClientName name = command.name() == null ? null : ClientName.of(command.name());
            final ClientEmail email = command.email() == null ? null : ClientEmail.of(command.email());
            final ClientPhoneNumber phone = command.phoneNumber() == null ? null : ClientPhoneNumber.of(command.phoneNumber());

            Client updatedClient = client;
            if (name != null) {
                updatedClient = updatedClient.changeName(name);
            }
            if (email != null) {
                updatedClient = updatedClient.changeEmail(email);
            }
            if (phone != null) {
                updatedClient = updatedClient.changePhone(phone);
            }

            return updatedClient == client ? client : clientRepository.save(updatedClient);
        }
    }
}


