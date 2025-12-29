package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.update.UpdateClient;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateClient Handler")
class UpdateClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private UpdateClient.Handler updateClientHandler;

    @Nested
    @DisplayName("Modification de client")
    class UpdateClientTest {

        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN met à jour tous les champs du client")
        void shouldUpdateAllClientFields() {
            // Given
            UUID clientId = UUID.randomUUID();
            String oldName = "John Doe";
            String oldEmail = "john.old@example.com";
            String oldPhone = "+33111111111";
            String newName = "John Updated";
            String newEmail = "john.new@example.com";
            String newPhone = "+33222222222";

            Person existingClient = Person.of(
                    ClientName.of(oldName),
                    ClientEmail.of(oldEmail),
                    ClientPhoneNumber.of(oldPhone),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    newName,
                    newEmail,
                    newPhone
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName().getValue()).isEqualTo(newName);
            assertThat(result.getEmail().getValue()).isEqualTo(newEmail.toLowerCase());
            assertThat(result.getPhone().getValue()).isEqualTo(newPhone);

            verify(clientService).findClientById(clientId);
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN utilise la méthode withCommonFields")
        void shouldUseWithCommonFieldsMethod() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            updateClientHandler.execute(command);

            // Then
            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository).save(clientCaptor.capture());

            Client savedClient = clientCaptor.getValue();
            assertThat(savedClient.getName().getValue()).isEqualTo("New Name");
            assertThat(savedClient.getEmail().getValue()).isEqualTo("new@example.com");
            assertThat(savedClient.getPhone().getValue()).isEqualTo("+33222222222");
        }

        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN récupère le client avant de le modifier")
        void shouldRetrieveClientBeforeUpdating() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Updated",
                    "john.updated@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            updateClientHandler.execute(command);

            // Then
            var ordered = inOrder(clientService, clientRepository);
            ordered.verify(clientService).findClientById(clientId);
            ordered.verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN nouvel email WHEN execute THEN met à jour l'email")
        void shouldUpdateEmail() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "new@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("GIVEN nouveau téléphone WHEN execute THEN met à jour le téléphone")
        void shouldUpdatePhone() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33999999999"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getPhone().getValue()).isEqualTo("+33999999999");
        }

        @Test
        @DisplayName("GIVEN nouveau nom WHEN execute THEN met à jour le nom")
        void shouldUpdateName() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "New Name",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("New Name");
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN client inexistant WHEN execute THEN lève ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(new ClientNotFoundException(clientId.toString()));

            // When & Then
            assertThatThrownBy(() -> updateClientHandler.execute(command))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining(clientId.toString());

            verify(clientService).findClientById(clientId);
            verify(clientRepository, never()).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN client introuvable WHEN execute THEN ne sauvegarde pas")
        void shouldNotSaveWhenClientNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            try {
                updateClientHandler.execute(command);
            } catch (ClientNotFoundException e) {
                // Expected exception
            }

            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("GIVEN email avec majuscules WHEN execute THEN normalise en minuscules")
        void shouldNormalizeEmailToLowercase() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "New@EXAMPLE.COM",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("GIVEN nom avec caractères spéciaux WHEN execute THEN met à jour correctement")
        void shouldUpdateNameWithSpecialCharacters() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "Jean-Pierre O'Connor",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Jean-Pierre O'Connor");
        }

        @Test
        @DisplayName("GIVEN numéro international WHEN execute THEN met à jour correctement")
        void shouldUpdateWithInternationalPhone() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+441234567890"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getPhone().getValue()).isEqualTo("+441234567890");
        }
    }
}

