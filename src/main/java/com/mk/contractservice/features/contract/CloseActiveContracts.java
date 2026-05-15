package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientDeletedEvent;
import com.mk.contractservice.domain.contract.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface CloseActiveContracts {

    record Command(UUID clientId, LocalDateTime closureDate) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(closureDate, "Closure date cannot be null");
        }
    }

    void execute(Command command);

    @Slf4j
    @Service
    @Transactional
    @RequiredArgsConstructor
    class Handler implements CloseActiveContracts {

        private final ContractRepository contractRepository;

        @Override
        public void execute(final Command command) {
            log.debug("Closing all active contracts for client: {} at {}", command.clientId(), command.closureDate());

            int closedCount = contractRepository.closeAllActiveByClientId(command.clientId(), command.closureDate());

            if (closedCount > 0) {
                log.info("Closed {} active contracts for client: {}", closedCount, command.clientId());
            } else {
                log.debug("No active contracts to close for client: {}", command.clientId());
            }
        }

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onClientDeleted(final ClientDeletedEvent event) {
            log.info("Client deleted event received for clientId: {}", event.getClientId());
            execute(new Command(event.getClientId(), event.getOccurredAt()));
        }
    }
}
