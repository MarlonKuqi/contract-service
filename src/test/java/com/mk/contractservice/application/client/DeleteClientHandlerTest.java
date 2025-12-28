package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.delete.DeleteClient;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteClientUseCase - Unit Tests")
class DeleteClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteClient.Handler deleteClient;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should delete client and publish event")
        void shouldDeleteClientAndPublishEvent() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            verify(clientRepository).deleteById(clientId);
            verify(eventPublisher).publishEvent(any(ClientDeletedEvent.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should delete client with correct ID")
        void shouldDeleteClientWithCorrectId() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            ArgumentCaptor<UUID> clientIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(clientRepository).deleteById(clientIdCaptor.capture());
            assertThat(clientIdCaptor.getValue()).isEqualTo(clientId);
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should publish event with correct client ID")
        void shouldPublishEventWithCorrectClientId() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            ArgumentCaptor<ClientDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ClientDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ClientDeletedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getClientId()).isEqualTo(clientId);
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should delete before publishing event")
        void shouldDeleteBeforePublishingEvent() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            var ordered = inOrder(clientRepository, eventPublisher);
            ordered.verify(clientRepository).deleteById(clientId);
            ordered.verify(eventPublisher).publishEvent(any(ClientDeletedEvent.class));
        }

        @Test
        @DisplayName("GIVEN multiple commands WHEN execute THEN should delete each client and publish events")
        void shouldDeleteMultipleClientsAndPublishEvents() {
            // Given
            UUID clientId1 = UUID.randomUUID();
            UUID clientId2 = UUID.randomUUID();
            UUID clientId3 = UUID.randomUUID();

            DeleteClient.Command command1 = new DeleteClient.Command(clientId1);
            DeleteClient.Command command2 = new DeleteClient.Command(clientId2);
            DeleteClient.Command command3 = new DeleteClient.Command(clientId3);

            doNothing().when(clientRepository).deleteById(any(UUID.class));
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command1);
            deleteClient.execute(command2);
            deleteClient.execute(command3);

            // Then
            verify(clientRepository).deleteById(clientId1);
            verify(clientRepository).deleteById(clientId2);
            verify(clientRepository).deleteById(clientId3);
            verify(clientRepository, times(3)).deleteById(any(UUID.class));

            verify(eventPublisher, times(3)).publishEvent(any(ClientDeletedEvent.class));
        }
    }

    @Nested
    @DisplayName("execute() - Event Publishing")
    class ExecuteEventPublishing {

        @Test
        @DisplayName("GIVEN successful deletion WHEN execute THEN should publish ClientDeletedEvent")
        void shouldPublishClientDeletedEvent() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            ArgumentCaptor<ClientDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ClientDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ClientDeletedEvent event = eventCaptor.getValue();
            assertThat(event).isNotNull();
            assertThat(event.getClientId()).isEqualTo(clientId);
        }

        @Test
        @DisplayName("GIVEN deletion WHEN execute THEN should publish exactly one event")
        void shouldPublishExactlyOneEvent() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            verify(eventPublisher, times(1)).publishEvent(any(ClientDeletedEvent.class));
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN event should contain same client ID as command")
        void eventShouldContainSameClientIdAsCommand() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            ArgumentCaptor<ClientDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ClientDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue().getClientId()).isSameAs(command.clientId());
        }
    }

    @Nested
    @DisplayName("execute() - Repository Interaction")
    class ExecuteRepositoryInteraction {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should call repository deleteById once")
        void shouldCallRepositoryDeleteByIdOnce() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            verify(clientRepository, times(1)).deleteById(clientId);
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN should pass client ID directly to repository")
        void shouldPassClientIdDirectlyToRepository() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(any(UUID.class));
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);

            // Then
            ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
            verify(clientRepository).deleteById(captor.capture());

            assertThat(captor.getValue()).isSameAs(command.clientId());
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN same client ID twice WHEN execute THEN should delete and publish event twice")
        void shouldDeleteAndPublishEventTwiceForSameClient() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When
            deleteClient.execute(command);
            deleteClient.execute(command);

            // Then
            verify(clientRepository, times(2)).deleteById(clientId);
            verify(eventPublisher, times(2)).publishEvent(any(ClientDeletedEvent.class));
        }

        @Test
        @DisplayName("GIVEN deletion WHEN execute THEN should not throw exception")
        void shouldNotThrowException() {
            // Given
            UUID clientId = UUID.randomUUID();
            DeleteClient.Command command = new DeleteClient.Command(clientId);

            doNothing().when(clientRepository).deleteById(clientId);
            doNothing().when(eventPublisher).publishEvent(any(ClientDeletedEvent.class));

            // When & Then - Should not throw any exception
            deleteClient.execute(command);

            verify(clientRepository).deleteById(clientId);
            verify(eventPublisher).publishEvent(any(ClientDeletedEvent.class));
        }
    }
}

