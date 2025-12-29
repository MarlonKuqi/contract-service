package com.mk.contractservice.application.feature.contract.closeactive;

import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.UUID;

public interface CloseActiveContracts {

    record Command(UUID clientId) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    void execute(Command command);

    @Slf4j
    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements CloseActiveContracts {

        ContractRepository contractRepository;

        @Override
        public void execute(final Command command) {
            log.debug("Closing all active contracts for client: {}", command.clientId());
            contractRepository.closeAllActiveByClientId(command.clientId());
            log.info("All active contracts closed for client: {}", command.clientId());
        }

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onClientDeleted(final ClientDeletedEvent event) {
            log.info("Client deleted event received for clientId: {}", event.getClientId());
            execute(new Command(event.getClientId()));
        }
    }
}
