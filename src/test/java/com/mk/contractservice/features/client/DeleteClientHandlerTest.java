package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientDeletedEvent;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteClient Handler")
class DeleteClientHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeleteClient.Handler deleteClientHandler;

    @BeforeEach
    void setUp() {
        ClientValidationService clientValidationService = new ClientValidationService(clientRepository);
        deleteClientHandler = new DeleteClient.Handler(clientRepository, clientValidationService, eventPublisher, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN client existant WHEN execute THEN supprime le client et publie l'événement au même instant")
    void shouldDeleteClientAndPublishEventAtSameInstant() {
        UUID clientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        when(clientRepository.existsById(clientId)).thenReturn(true);

        deleteClientHandler.execute(new DeleteClient.Command(clientId));

        verify(clientRepository).delete(clientId, now);
        verify(eventPublisher).publishEvent(ClientDeletedEvent.of(clientId, now));
    }

    @Test
    @DisplayName("GIVEN client inexistant ou déjà supprimé WHEN execute THEN lève ClientNotFoundException sans rien publier")
    void shouldRejectUnknownClient() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.existsById(clientId)).thenReturn(false);

        assertThatThrownBy(() -> deleteClientHandler.execute(new DeleteClient.Command(clientId)))
                .isInstanceOf(ClientNotFoundException.class);

        verify(clientRepository, never()).delete(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
