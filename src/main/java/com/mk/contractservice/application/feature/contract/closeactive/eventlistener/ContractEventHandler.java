package com.mk.contractservice.application.feature.contract.closeactive.eventlistener;

import com.mk.contractservice.application.feature.contract.closeactive.core.CloseActiveContracts;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ContractEventHandler {

    CloseActiveContracts closeActiveContractsUseCase;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onClientDeleted(final ClientDeletedEvent event) {
        log.info("Client deleted event received for clientId: {}. Closing all active contracts...", event.getClientId());

        final CloseActiveContracts.Command command = new CloseActiveContracts.Command(event.getClientId());
        closeActiveContractsUseCase.execute(command);

        log.info("All active contracts closed for clientId: {}", event.getClientId());
    }
}

