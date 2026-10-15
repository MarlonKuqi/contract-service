package com.mk.contractservice.domain.client;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientDeletedEvent {
    UUID clientId;
    LocalDateTime occurredAt;

    public static ClientDeletedEvent of(final UUID clientId, final LocalDateTime occurredAt) {
        return new ClientDeletedEvent(notNull(clientId), notNull(occurredAt));
    }
}
