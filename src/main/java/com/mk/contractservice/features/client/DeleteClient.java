package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface DeleteClient {

    record Command(UUID clientId) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    void execute(final Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements DeleteClient {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        public void execute(final Command command) {
            clientRepository.ensureExists(command.clientId());
            final LocalDateTime deletedAt = LocalDateTime.now(clock);
            clientRepository.delete(command.clientId(), deletedAt);
            contractRepository.closeAllActiveByClientId(command.clientId(), deletedAt);
        }
    }
}

