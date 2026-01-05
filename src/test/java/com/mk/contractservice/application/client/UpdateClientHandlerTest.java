package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.update.UpdateClient;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.PhoneAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateClient Handler")
class UpdateClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;
    private ClientValidationService clientValidationService;
    private UpdateClient.Handler updateClientHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        updateClientHandler = new UpdateClient.Handler(clientRepository, clientService, clientValidationService);
    }


    @Nested
    @DisplayName("Person - Mise à jour réussie")
    class PersonUpdateSuccess {

        private UUID clientId;
        private Person existingPerson;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingPerson = Person.builder()
                    .id(clientId)
                    .name(ClientName.of("John Doe"))
                    .email(ClientEmail.of("john@example.com"))
                    .phone(ClientPhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

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
    }

    @Nested
    @DisplayName("Company - Mise à jour réussie")
    class CompanyUpdateSuccess {

        private UUID clientId;
        private Company existingCompany;

        @BeforeEach
        void setUp() {
            clientId = UUID.randomUUID();
            existingCompany = Company.builder()
                    .id(clientId)
                    .name(ClientName.of("Acme Corp"))
                    .email(ClientEmail.of("contact@acme.com"))
                    .phone(ClientPhoneNumber.of("+33111111111"))
                    .companyIdentifier(CompanyIdentifier.of("123456789"))
                    .build();

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
            existingPerson = Person.builder()
                    .id(clientId)
                    .name(ClientName.of("John Doe"))
                    .email(ClientEmail.of("john@example.com"))
                    .phone(ClientPhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();
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

        @Test
        @DisplayName("GIVEN email déjà utilisé WHEN execute THEN lève EmailAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "duplicate@example.com";

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    duplicateEmail,
                    "+33111111111"
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);
            when(clientRepository.existsByEmail(duplicateEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> updateClientHandler.execute(command))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("GIVEN téléphone déjà utilisé WHEN execute THEN lève PhoneAlreadyExistsException")
        void shouldThrowExceptionWhenPhoneAlreadyExists() {
            // Given
            String duplicatePhone = "+33999999999";

            UpdateClient.Command command = new UpdateClient.Command(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    duplicatePhone
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);
            when(clientRepository.existsByPhoneNumber(duplicatePhone)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> updateClientHandler.execute(command))
                    .isInstanceOf(PhoneAlreadyExistsException.class);
        }
    }
}

