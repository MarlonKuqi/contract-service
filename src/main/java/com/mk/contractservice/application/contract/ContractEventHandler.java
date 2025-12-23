package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.CloseActiveContractsUseCase;
import com.mk.contractservice.application.contract.usecase.CloseActiveContractsUseCase.CloseActiveContractsCommand;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ContractEventHandler {

    CloseActiveContractsUseCase closeActiveContractsUseCase;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @CacheEvict(value = "contractSums", key = "#event.clientId")
    public void onClientDeleted(final ClientDeletedEvent event) {
        log.info("Client deleted event received for clientId: {}. Closing all active contracts...",
                event.clientId());

        final CloseActiveContractsUseCase.CloseActiveContractsCommand command = new CloseActiveContractsCommand(event.clientId());
        closeActiveContractsUseCase.execute(command);

        log.info("All active contracts closed for clientId: {}", event.clientId());
    }
}



