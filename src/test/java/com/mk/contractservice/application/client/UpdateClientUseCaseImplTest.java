package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.UpdateClientUseCase.UpdateClientCommand;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
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
@DisplayName("UpdateClientUseCase - Unit Tests")
class UpdateClientUseCaseImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private UpdateClientUseCaseImpl updateClientUseCase;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should update all client fields")
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

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    newName,
                    newEmail,
                    newPhone
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName().value()).isEqualTo(newName);
            assertThat(result.getEmail().value()).isEqualTo(newEmail.toLowerCase());
            assertThat(result.getPhone().value()).isEqualTo(newPhone);

            verify(clientService).findClientById(clientId);
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should use withCommonFields method")
        void shouldUseWithCommonFieldsMethod() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            updateClientUseCase.execute(command);

            // Then
            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository).save(clientCaptor.capture());

            Client savedClient = clientCaptor.getValue();
            assertThat(savedClient.getName().value()).isEqualTo("New Name");
            assertThat(savedClient.getEmail().value()).isEqualTo("new@example.com");
            assertThat(savedClient.getPhone().value()).isEqualTo("+33222222222");
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should retrieve client before updating")
        void shouldRetrieveClientBeforeUpdating() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "John Updated",
                    "john.updated@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            updateClientUseCase.execute(command);

            // Then
            var ordered = inOrder(clientService, clientRepository);
            ordered.verify(clientService).findClientById(clientId);
            ordered.verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN command with new email WHEN execute THEN should update email")
        void shouldUpdateEmail() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "John Doe",
                    "new@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getEmail().value()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("GIVEN command with new phone WHEN execute THEN should update phone")
        void shouldUpdatePhone() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33999999999"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getPhone().value()).isEqualTo("+33999999999");
        }

        @Test
        @DisplayName("GIVEN command with new name WHEN execute THEN should update name")
        void shouldUpdateName() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "New Name",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getName().value()).isEqualTo("New Name");
        }
    }

    @Nested
    @DisplayName("execute() - Validation Errors")
    class ExecuteValidationErrors {

        @Test
        @DisplayName("GIVEN non-existent client WHEN execute THEN should throw ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(new ClientNotFoundException(clientId.toString()));

            // When & Then
            assertThatThrownBy(() -> updateClientUseCase.execute(command))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining(clientId.toString());

            verify(clientService).findClientById(clientId);
            verify(clientRepository, never()).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN client not found WHEN execute THEN should not save")
        void shouldNotSaveWhenClientNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            try {
                updateClientUseCase.execute(command);
            } catch (ClientNotFoundException e) {
                // Expected exception
            }

            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN email with uppercase WHEN execute THEN should normalize to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "John Doe",
                    "New@EXAMPLE.COM",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getEmail().value()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("GIVEN name with special characters WHEN execute THEN should update correctly")
        void shouldUpdateNameWithSpecialCharacters() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "Jean-Pierre O'Connor",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getName().value()).isEqualTo("Jean-Pierre O'Connor");
        }

        @Test
        @DisplayName("GIVEN international phone number WHEN execute THEN should update correctly")
        void shouldUpdateWithInternationalPhone() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            UpdateClientCommand command = new UpdateClientCommand(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+441234567890"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = updateClientUseCase.execute(command);

            // Then
            assertThat(result.getPhone().value()).isEqualTo("+441234567890");
        }
    }
}

