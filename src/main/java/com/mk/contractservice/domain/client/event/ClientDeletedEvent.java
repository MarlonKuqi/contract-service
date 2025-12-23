package com.mk.contractservice.domain.client.event;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ClientDeletedEvent {

    private final UUID clientId;
    private final LocalDateTime occurredAt;

    private ClientDeletedEvent(final UUID clientId, final LocalDateTime occurredAt) {
        this.clientId = clientId;
        this.occurredAt = occurredAt;
    }

    public static ClientDeletedEvent of(final UUID clientId) {
        return new ClientDeletedEvent(clientId, LocalDateTime.now());
    }

    public UUID clientId() {
        return clientId;
    }

    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}

