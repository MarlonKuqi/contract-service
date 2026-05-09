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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteClient Handler")
class DeleteClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeleteClient.Handler deleteClientHandler;

    @BeforeEach
    void setUp() {
        deleteClientHandler = new DeleteClient.Handler(clientRepository, eventPublisher);
    }

    @Test
    @DisplayName("GIVEN commande valide WHEN execute THEN publie un événement ClientDeletedEvent")
    void shouldPublishClientDeletedEvent() {
        // Given
        UUID clientId = UUID.randomUUID();
        DeleteClient.Command command = new DeleteClient.Command(clientId);

        // When
        deleteClientHandler.execute(command);

        // Then
        verify(eventPublisher).publishEvent(any(ClientDeletedEvent.class));
    }
}

