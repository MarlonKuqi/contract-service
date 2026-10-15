package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientDeletedEvent;
import com.mk.contractservice.domain.client.ClientRepository;
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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteClient Handler")
class DeleteClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private DeleteClient.Handler deleteClientHandler;

    @BeforeEach
    void setUp() {
        deleteClientHandler = new DeleteClient.Handler(clientRepository, eventPublisher, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN commande valide WHEN execute THEN publie un ClientDeletedEvent daté par l'horloge")
    void shouldPublishClientDeletedEventDatedByClock() {
        UUID clientId = UUID.randomUUID();
        DeleteClient.Command command = new DeleteClient.Command(clientId);

        deleteClientHandler.execute(command);

        verify(eventPublisher).publishEvent(ClientDeletedEvent.of(clientId, LocalDateTime.now(FIXED_CLOCK)));
    }
}

