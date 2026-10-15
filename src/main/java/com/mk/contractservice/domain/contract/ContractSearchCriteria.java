package com.mk.contractservice.domain.contract;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ContractSearchCriteria(
        UUID clientId,
        @Nullable LocalDateTime activeAt,
        @Nullable LocalDateTime updatedSince
) {

    public ContractSearchCriteria {
        Objects.requireNonNull(clientId, "clientId must not be null");
    }

    public static ContractSearchCriteria activeForClient(final UUID clientId, final LocalDateTime activeAt) {
        Objects.requireNonNull(activeAt, "activeAt must not be null");
        return new ContractSearchCriteria(clientId, activeAt, null);
    }

    public static ContractSearchCriteria activeForClientUpdatedSince(
            final UUID clientId,
            final LocalDateTime activeAt,
            final LocalDateTime updatedSince
    ) {
        Objects.requireNonNull(activeAt, "activeAt must not be null");
        Objects.requireNonNull(updatedSince, "updatedSince must not be null");
        return new ContractSearchCriteria(clientId, activeAt, updatedSince);
    }
}
