package com.mk.contractservice.application.client.usecase;

import com.mk.contractservice.domain.client.aggregate.Client;

import java.util.Objects;
import java.util.UUID;

public interface UpdateClientUseCase {

    Client execute(UpdateClientCommand command);

    record UpdateClientCommand(
            UUID clientId,
            String name,
            String email,
            String phoneNumber
    ) {
        public UpdateClientCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        }
    }
}

