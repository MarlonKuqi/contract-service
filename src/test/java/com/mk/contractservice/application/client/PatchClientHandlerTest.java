package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.patch.core.PatchClient;
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
@DisplayName("PatchClientUseCase - Unit Tests")
class PatchClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private PatchClient.Handler patchClient;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN all fields provided WHEN execute THEN should update all fields")
        void shouldUpdateAllFieldsWhenAllProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("New Name");
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo("+33222222222");
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN only name provided WHEN execute THEN should update only name")
        void shouldUpdateOnlyNameWhenOnlyNameProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalEmail = "john@example.com";
            String originalPhone = "+33111111111";

            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of(originalEmail),
                    ClientPhoneNumber.of(originalPhone),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    null,
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("New Name");
            assertThat(result.getEmail().getValue()).isEqualTo(originalEmail);
            assertThat(result.getPhone().getValue()).isEqualTo(originalPhone);
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN only email provided WHEN execute THEN should update only email")
        void shouldUpdateOnlyEmailWhenOnlyEmailProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalName = "John Doe";
            String originalPhone = "+33111111111";

            Person existingClient = Person.of(
                    ClientName.of(originalName),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of(originalPhone),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    null,
                    "new@example.com",
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(originalName);
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo(originalPhone);
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN only phone provided WHEN execute THEN should update only phone")
        void shouldUpdateOnlyPhoneWhenOnlyPhoneProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalName = "John Doe";
            String originalEmail = "john@example.com";

            Person existingClient = Person.of(
                    ClientName.of(originalName),
                    ClientEmail.of(originalEmail),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    null,
                    null,
                    "+33999999999"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(originalName);
            assertThat(result.getEmail().getValue()).isEqualTo(originalEmail);
            assertThat(result.getPhone().getValue()).isEqualTo("+33999999999");
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN name and email provided WHEN execute THEN should update name and email")
        void shouldUpdateNameAndEmailWhenBothProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalPhone = "+33111111111";

            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of(originalPhone),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("New Name");
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo(originalPhone);
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN name and phone provided WHEN execute THEN should update name and phone")
        void shouldUpdateNameAndPhoneWhenBothProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalEmail = "john@example.com";

            Person existingClient = Person.of(
                    ClientName.of("Old Name"),
                    ClientEmail.of(originalEmail),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    null,
                    "+33999999999"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("New Name");
            assertThat(result.getEmail().getValue()).isEqualTo(originalEmail);
            assertThat(result.getPhone().getValue()).isEqualTo("+33999999999");
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN email and phone provided WHEN execute THEN should update email and phone")
        void shouldUpdateEmailAndPhoneWhenBothProvided() {
            // Given
            UUID clientId = UUID.randomUUID();
            String originalName = "John Doe";

            Person existingClient = Person.of(
                    ClientName.of(originalName),
                    ClientEmail.of("old@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    null,
                    "new@example.com",
                    "+33999999999"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(originalName);
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo("+33999999999");
            verify(clientRepository).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("execute() - No Changes")
    class ExecuteNoChanges {

        @Test
        @DisplayName("GIVEN all fields null WHEN execute THEN should return client unchanged and not save")
        void shouldReturnUnchangedClientWhenAllFieldsNull() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    null,
                    null,
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result).isSameAs(existingClient);
            verify(clientRepository, never()).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN all fields null WHEN execute THEN should retrieve client but not save")
        void shouldRetrieveClientButNotSaveWhenNoChanges() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(clientId, null, null, null);

            when(clientService.findClientById(clientId)).thenReturn(existingClient);

            // When
            patchClient.execute(command);

            // Then
            verify(clientService).findClientById(clientId);
            verify(clientRepository, never()).save(any(Client.class));
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
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    null,
                    null
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            assertThatThrownBy(() -> patchClient.execute(command))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining(clientId.toString());

            verify(clientService).findClientById(clientId);
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

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    null,
                    "New@EXAMPLE.COM",
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo("new@example.com");
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

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "Jean-Pierre O'Connor",
                    null,
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Client result = patchClient.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Jean-Pierre O'Connor");
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN should retrieve client before patching")
        void shouldRetrieveClientBeforePatching() {
            // Given
            UUID clientId = UUID.randomUUID();
            Person existingClient = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    "New Name",
                    null,
                    null
            );

            when(clientService.findClientById(clientId)).thenReturn(existingClient);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            patchClient.execute(command);

            // Then
            var ordered = inOrder(clientService, clientRepository);
            ordered.verify(clientService).findClientById(clientId);
            ordered.verify(clientRepository).save(any(Client.class));
        }
    }
}

