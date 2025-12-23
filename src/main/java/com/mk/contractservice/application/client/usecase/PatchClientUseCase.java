package com.mk.contractservice.application.client.usecase;

import com.mk.contractservice.domain.client.aggregate.Client;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public interface PatchClientUseCase {

    Client execute(PatchClientCommand command);

    record PatchClientCommand(
            UUID clientId,
            @Nullable String name,
            @Nullable String email,
            @Nullable String phoneNumber
    ) {
        public PatchClientCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }
}

