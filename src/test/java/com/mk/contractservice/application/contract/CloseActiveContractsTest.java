package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.feature.contract.closeactive.CloseActiveContracts;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseActiveContracts")
class CloseActiveContractsTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private CloseActiveContracts.Handler handler;

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("should close all active contracts for client")
        void shouldCloseAllActiveContracts() {
            UUID clientId = UUID.randomUUID();
            CloseActiveContracts.Command command = new CloseActiveContracts.Command(clientId);

            handler.execute(command);

            verify(contractRepository).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("should pass correct clientId to repository")
        void shouldPassCorrectClientId() {
            UUID clientId = UUID.randomUUID();
            CloseActiveContracts.Command command = new CloseActiveContracts.Command(clientId);

            handler.execute(command);

            ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
            verify(contractRepository).closeAllActiveByClientId(captor.capture());
            assertThat(captor.getValue()).isEqualTo(clientId);
        }

        @Test
        @DisplayName("should handle multiple clients separately")
        void shouldHandleMultipleClients() {
            UUID clientId1 = UUID.randomUUID();
            UUID clientId2 = UUID.randomUUID();
            UUID clientId3 = UUID.randomUUID();

            handler.execute(new CloseActiveContracts.Command(clientId1));
            handler.execute(new CloseActiveContracts.Command(clientId2));
            handler.execute(new CloseActiveContracts.Command(clientId3));

            verify(contractRepository).closeAllActiveByClientId(clientId1);
            verify(contractRepository).closeAllActiveByClientId(clientId2);
            verify(contractRepository).closeAllActiveByClientId(clientId3);
        }

        @Test
        @DisplayName("should handle same client multiple times")
        void shouldHandleSameClientMultipleTimes() {
            UUID clientId = UUID.randomUUID();
            CloseActiveContracts.Command command = new CloseActiveContracts.Command(clientId);

            handler.execute(command);
            handler.execute(command);

            verify(contractRepository, times(2)).closeAllActiveByClientId(clientId);
        }
    }

    @Nested
    @DisplayName("onClientDeleted()")
    class OnClientDeleted {

        @Test
        @DisplayName("should close all active contracts when client deleted")
        void shouldCloseContractsOnEvent() {
            UUID clientId = UUID.randomUUID();
            ClientDeletedEvent event = ClientDeletedEvent.of(clientId);

            handler.onClientDeleted(event);

            verify(contractRepository).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("should handle multiple deletion events")
        void shouldHandleMultipleEvents() {
            UUID clientId1 = UUID.randomUUID();
            UUID clientId2 = UUID.randomUUID();

            handler.onClientDeleted(ClientDeletedEvent.of(clientId1));
            handler.onClientDeleted(ClientDeletedEvent.of(clientId2));

            verify(contractRepository).closeAllActiveByClientId(clientId1);
            verify(contractRepository).closeAllActiveByClientId(clientId2);
        }
    }
}

