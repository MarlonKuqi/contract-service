package com.mk.contractservice.domain.client;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientDeletedEvent {
    UUID clientId;
    LocalDateTime occurredAt;

    public static ClientDeletedEvent of(final UUID clientId) {
        return new ClientDeletedEvent(clientId, LocalDateTime.now());
    }
}

