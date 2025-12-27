package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.feature.contract.closeactive.core.CloseActiveContracts;
import com.mk.contractservice.application.feature.contract.closeactive.eventlistener.ContractEventHandler;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("ContractEventHandler - Domain Events Tests")
class ContractEventHandlerTest {

    @Mock
    private CloseActiveContracts.Handler closeActiveContracts;

    @InjectMocks
    private ContractEventHandler handler;

    @Test
    @DisplayName("GIVEN ClientDeletedEvent WHEN onClientDeleted THEN should close all active contracts")
    void shouldCloseAllActiveContractsWhenClientDeleted() {
        // Given
        UUID clientId = UUID.randomUUID();
        ClientDeletedEvent event = ClientDeletedEvent.of(clientId);

        // When
        handler.onClientDeleted(event);

        // Then
        ArgumentCaptor<CloseActiveContracts.Command> commandCaptor = ArgumentCaptor.forClass(CloseActiveContracts.Command.class);
        verify(closeActiveContracts).execute(commandCaptor.capture());

        assertThat(commandCaptor.getValue().clientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("GIVEN ClientDeletedEvent with different clientId WHEN onClientDeleted THEN should close contracts for correct client")
    void shouldCloseContractsForCorrectClient() {
        // Given
        UUID clientId1 = UUID.randomUUID();
        UUID clientId2 = UUID.randomUUID();
        ClientDeletedEvent event1 = ClientDeletedEvent.of(clientId1);
        ClientDeletedEvent event2 = ClientDeletedEvent.of(clientId2);

        // When
        handler.onClientDeleted(event1);
        handler.onClientDeleted(event2);

        // Then
        ArgumentCaptor<CloseActiveContracts.Command> commandCaptor = ArgumentCaptor.forClass(CloseActiveContracts.Command.class);
        verify(closeActiveContracts, times(2)).execute(commandCaptor.capture());

        assertThat(commandCaptor.getAllValues()).hasSize(2);
        assertThat(commandCaptor.getAllValues().get(0).clientId()).isEqualTo(clientId1);
        assertThat(commandCaptor.getAllValues().get(1).clientId()).isEqualTo(clientId2);
    }
}
