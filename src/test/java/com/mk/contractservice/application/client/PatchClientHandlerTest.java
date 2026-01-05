package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.patch.PatchClient;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchClient Handler")
class PatchClientHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientService clientService;

    private ClientValidationService clientValidationService;
    private PatchClient.Handler patchClientHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        patchClientHandler = new PatchClient.Handler(clientRepository, clientService, clientValidationService);
    }

    @Nested
    @DisplayName("Person - Mise à jour partielle")
    class PersonPatchTest {

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
        @DisplayName("GIVEN tous les champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllFieldsWhenAllProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.of("Jane Smith"),
                    Optional.of("jane@example.com"),
                    Optional.of("+33222222222")
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Jane Smith");
            assertThat(result.getEmail().getValue()).isEqualTo("jane@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo("+33222222222");
        }

        @Test
        @DisplayName("GIVEN seulement nom WHEN execute THEN met à jour uniquement le nom")
        void shouldUpdateOnlyNameWhenOnlyNameProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.of("Jane Smith"),
                    Optional.empty(),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Jane Smith");
            assertThat(result.getEmail().getValue()).isEqualTo(existingPerson.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingPerson.getPhone().getValue());
        }

        @Test
        @DisplayName("GIVEN seulement email WHEN execute THEN met à jour uniquement l'email")
        void shouldUpdateOnlyEmailWhenOnlyEmailProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.of("jane@example.com"),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingPerson.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo("jane@example.com");
            assertThat(result.getPhone().getValue()).isEqualTo(existingPerson.getPhone().getValue());
        }

        @Test
        @DisplayName("GIVEN seulement téléphone WHEN execute THEN met à jour uniquement le téléphone")
        void shouldUpdateOnlyPhoneWhenOnlyPhoneProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("+33999999999")
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingPerson.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo(existingPerson.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo("+33999999999");
        }
    }

    @Nested
    @DisplayName("Aucune mise à jour")
    class NoUpdateTest {

        @Test
        @DisplayName("GIVEN aucun champ WHEN execute on Person THEN ne change rien")
        void shouldNotChangeAnythingWhenNoFieldsProvidedForPerson() {
            // Given
            final UUID clientId = UUID.randomUUID();
            final Person existingPerson = Person.builder()
                    .id(clientId)
                    .name(ClientName.of("John Doe"))
                    .email(ClientEmail.of("john@example.com"))
                    .phone(ClientPhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingPerson.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo(existingPerson.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingPerson.getPhone().getValue());
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN aucun champ WHEN execute on Company THEN ne change rien")
        void shouldNotChangeAnythingWhenNoFieldsProvidedForCompany() {
            // Given
            final UUID clientId = UUID.randomUUID();
            final Company existingCompany = Company.builder()
                    .id(clientId)
                    .name(ClientName.of("Acme Corp"))
                    .email(ClientEmail.of("contact@acme.com"))
                    .phone(ClientPhoneNumber.of("+33111111111"))
                    .companyIdentifier(CompanyIdentifier.of("123456789"))
                    .build();
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingCompany);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingCompany.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo(existingCompany.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingCompany.getPhone().getValue());
            assertThat(((Company) result).getCompanyIdentifier()).isEqualTo(existingCompany.getCompanyIdentifier());
        }
    }

    @Nested
    @DisplayName("Company - Mise à jour partielle")
    class CompanyPatchTest {

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
        @DisplayName("GIVEN tous les champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllFieldsWhenAllProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.of("Acme Corporation"),
                    Optional.of("info@acme.com"),
                    Optional.of("+33222222222")
            );

            when(clientService.findClientById(clientId)).thenReturn(existingCompany);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Acme Corporation");
            assertThat(result.getEmail().getValue()).isEqualTo("info@acme.com");
            assertThat(result.getPhone().getValue()).isEqualTo("+33222222222");
            assertThat(((Company) result).getCompanyIdentifier()).isEqualTo(existingCompany.getCompanyIdentifier());
        }

        @Test
        @DisplayName("GIVEN seulement nom WHEN execute THEN met à jour uniquement le nom")
        void shouldUpdateOnlyNameWhenOnlyNameProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.of("Acme Corporation"),
                    Optional.empty(),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingCompany);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Acme Corporation");
            assertThat(result.getEmail().getValue()).isEqualTo(existingCompany.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingCompany.getPhone().getValue());
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
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.of("New Name"),
                    Optional.empty(),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            assertThatThrownBy(() -> patchClientHandler.execute(command))
                    .isInstanceOf(ClientNotFoundException.class);
        }

        @Test
        @DisplayName("GIVEN email déjà utilisé WHEN execute THEN lève EmailAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "duplicate@example.com";
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.of(duplicateEmail),
                    Optional.empty()
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);
            when(clientRepository.existsByEmail(duplicateEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> patchClientHandler.execute(command))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("GIVEN téléphone déjà utilisé WHEN execute THEN lève PhoneAlreadyExistsException")
        void shouldThrowExceptionWhenPhoneAlreadyExists() {
            // Given
            String duplicatePhone = "+33999999999";
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(duplicatePhone)
            );

            when(clientService.findClientById(clientId)).thenReturn(existingPerson);
            when(clientRepository.existsByPhoneNumber(duplicatePhone)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> patchClientHandler.execute(command))
                    .isInstanceOf(PhoneAlreadyExistsException.class);
        }

    }
}

