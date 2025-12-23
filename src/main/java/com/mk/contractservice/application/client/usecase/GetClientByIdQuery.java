package com.mk.contractservice.application.client.usecase;

import com.mk.contractservice.domain.client.aggregate.Client;

import java.util.Objects;
import java.util.UUID;

public interface GetClientByIdQuery {

    Client execute(GetClientQuery query);

    record GetClientQuery(UUID clientId) {
        public GetClientQuery {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }
}


