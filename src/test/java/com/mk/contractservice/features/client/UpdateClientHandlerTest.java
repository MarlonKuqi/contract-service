package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyFactory;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonFactory;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private UpdateClient.Handler updateClientHandler;

    @BeforeEach
    void setUp() {
        updateClientHandler = new UpdateClient.Handler(clientRepository, clientService);
    }


    @Nested
    @DisplayName("Person - Mise à jour réussie")
    class PersonUpdateSuccess {

        private UUID clientId;
        private Person existingPerson;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingPerson = PersonFactory.buildFromDatabase(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33111111111",
                    LocalDate.of(1990, 1, 1)
            );

            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("GIVEN person valide avec nouveaux champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllPersonFields() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "Jane Smith",
                    "jane.new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN person avec nom et téléphone changés WHEN execute THEN met à jour les champs")
        void shouldUpdateNameAndPhone() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Updated",
                    "john@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN person avec nom et email changés WHEN execute THEN met à jour les champs")
        void shouldUpdateNameAndEmail() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Updated",
                    "john.new@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN person avec uniquement nom changé WHEN execute THEN met à jour le nom")
        void shouldUpdateOnlyName() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Smith",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN un champ modifié WHEN execute THEN sauvegarde le client modifié")
        void shouldSaveWhenOneFieldChanges() {
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            Client result = updateClientHandler.execute(command);

            verify(clientRepository).save(result);
            assertThat(result).isNotSameAs(existingPerson);
        }
    }

    @Nested
    @DisplayName("Aucune modification")
    class NoChange {

        private UUID clientId;
        private Person existingPerson;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingPerson = PersonFactory.buildFromDatabase(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33111111111",
                    LocalDate.of(1990, 1, 1)
            );
        }

        @Test
        @DisplayName("GIVEN valeurs identiques WHEN execute THEN ne sauvegarde pas et renvoie le client existant")
        void shouldNotSaveWhenValuesAreIdentical() {
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            Client result = updateClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingPerson);
        }

        @Test
        @DisplayName("GIVEN mêmes valeurs écrites autrement WHEN execute THEN ne sauvegarde pas et conserve les valeurs normalisées")
        void shouldNotSaveWhenSameValuesWrittenDifferently() {
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "  John Doe  ",
                    "JOHN@Example.com",
                    "+33 1 11 11 11 11"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            Client result = updateClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingPerson);
        }
    }

    @Nested
    @DisplayName("Company - Mise à jour réussie")
    class CompanyUpdateSuccess {

        private UUID clientId;
        private Company existingCompany;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingCompany = CompanyFactory.buildFromDatabase(
                    clientId,
                    "Acme Corp",
                    "contact@acme.com",
                    "+33111111111",
                    "123456789"
            );

            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("GIVEN company valide avec nouveaux champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllCompanyFields() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "Acme Corporation",
                    "contact.new@acme.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingCompany);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Company) result).getCompanyIdentifier()).isEqualTo(existingCompany.getCompanyIdentifier());
        }

        @Test
        @DisplayName("GIVEN company avec uniquement nom changé WHEN execute THEN met à jour le nom")
        void shouldUpdateOnlyName() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "Acme Corporation",
                    "contact@acme.com",
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingCompany);

            // When
            Client result = updateClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(((Company) result).getCompanyIdentifier()).isEqualTo(existingCompany.getCompanyIdentifier());
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        private UUID clientId;
        private Person existingPerson;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingPerson = PersonFactory.buildFromDatabase(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33111111111",
                    LocalDate.of(1990, 1, 1)
            );
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN execute THEN lève ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            // Given
            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "New Name",
                    "new@example.com",
                    "+33222222222"
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            assertThatThrownBy(() -> updateClientHandler.execute(command))
                    .isInstanceOf(ClientNotFoundException.class);
        }
    }
}

