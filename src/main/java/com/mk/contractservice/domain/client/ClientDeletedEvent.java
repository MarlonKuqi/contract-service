package com.mk.contractservice.domain.client;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientDeletedEvent {
    UUID clientId;
    LocalDateTime occurredAt;

    public static ClientDeletedEvent of(final UUID clientId) {
        return new ClientDeletedEvent(clientId, LocalDateTime.now());
    }
}

