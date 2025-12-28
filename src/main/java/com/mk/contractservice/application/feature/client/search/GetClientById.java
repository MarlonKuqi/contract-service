package com.mk.contractservice.application.feature.client.search;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public sealed interface GetClientById permits GetClientById.Handler {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    Client execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements GetClientById {

        ClientService clientService;

        @Override
        public Client execute(final Query query) {
            return clientService.findClientById(query.clientId());
        }
    }
}

