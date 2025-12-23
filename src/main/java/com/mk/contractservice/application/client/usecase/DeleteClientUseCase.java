package com.mk.contractservice.application.client.usecase;

import java.util.Objects;
import java.util.UUID;

public interface DeleteClientUseCase {

    void execute(DeleteClientCommand command);

    record DeleteClientCommand(UUID clientId) {
        public DeleteClientCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }
}

