package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientDetails;
import com.mk.contractservice.domain.client.ClientQueryPort;
import com.mk.contractservice.features.shared.QueryUseCase;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

public interface GetClientById {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    ClientDetails execute(Query query);

    @QueryUseCase
    @RequiredArgsConstructor
    class Handler implements GetClientById {

        private final ClientQueryPort clientQueryPort;

        @Override
        public ClientDetails execute(final Query query) {
            return clientQueryPort.getDetails(query.clientId());
        }
    }
}
