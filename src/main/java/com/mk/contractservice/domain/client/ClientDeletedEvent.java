package com.mk.contractservice.domain.client;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class ClientDeletedEvent {
    UUID clientId;
    LocalDateTime occurredAt;

    public static ClientDeletedEvent of(final UUID clientId) {
        return new ClientDeletedEvent(clientId, LocalDateTime.now());
    }
}

