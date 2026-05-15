package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientDeletedEvent;
import com.mk.contractservice.domain.client.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface DeleteClient {

    record Command(UUID clientId) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    void execute(final Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    class Handler implements DeleteClient {

        private final ClientRepository clientRepository;
        private final ApplicationEventPublisher eventPublisher;

        @Override
        public void execute(final Command command) {
            clientRepository.deleteById(command.clientId());
            eventPublisher.publishEvent(ClientDeletedEvent.of(command.clientId()));
        }
    }
}

