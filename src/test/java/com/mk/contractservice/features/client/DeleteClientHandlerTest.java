package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteClient Handler")
class DeleteClientHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private DeleteClient.Handler deleteClientHandler;

    @BeforeEach
    void setUp() {
        deleteClientHandler = new DeleteClient.Handler(clientRepository, contractRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN client existant WHEN execute THEN supprime le client puis ferme ses contrats actifs au même instant")
    void shouldDeleteClientThenCloseItsActiveContractsAtSameInstant() {
        UUID clientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);

        deleteClientHandler.execute(new DeleteClient.Command(clientId));

        InOrder inOrder = inOrder(clientRepository, contractRepository);
        inOrder.verify(clientRepository).delete(clientId, now);
        inOrder.verify(contractRepository).closeAllActiveByClientId(clientId, now);
    }

    @Test
    @DisplayName("GIVEN client inexistant ou déjà supprimé WHEN execute THEN lève ClientNotFoundException sans rien modifier")
    void shouldRejectUnknownClient() {
        UUID clientId = UUID.randomUUID();
        doThrow(ClientNotFoundException.forId(clientId)).when(clientRepository).ensureExists(clientId);

        assertThatThrownBy(() -> deleteClientHandler.execute(new DeleteClient.Command(clientId)))
                .isInstanceOf(ClientNotFoundException.class);

        verify(clientRepository, never()).delete(any(), any());
        verify(contractRepository, never()).closeAllActiveByClientId(any(), any());
    }
}
