package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface GetClientById {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    Client execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements GetClientById {

        private final ClientService clientService;

        @Override
        public Client execute(final Query query) {
            return clientService.findClientById(query.clientId());
        }
    }
}

