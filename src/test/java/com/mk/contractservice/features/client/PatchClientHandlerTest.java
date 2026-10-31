package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyFactory;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonFactory;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.domain.shared.exception.VersionMismatchException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchClient Handler")
class PatchClientHandlerTest {

    private static final long VERSION = 3L;

    @Mock
    private ClientRepository clientRepository;

    private PatchClient.Handler patchClientHandler;

    @BeforeEach
    void setUp() {
        patchClientHandler = new PatchClient.Handler(clientRepository);
    }

    @Nested
    @DisplayName("Person - Mise à jour partielle")
    class PersonPatchTest {

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
        @DisplayName("GIVEN tous les champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllFieldsWhenAllProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    VERSION,
                    "Jane Smith",
                    "jane@example.com",
                    "+33222222222"
            );

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

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
                    VERSION,
                    "Jane Smith",
                    null,
                    null
            );

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

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
                    VERSION,
                    null,
                    "jane@example.com",
                    null
            );

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

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
                    VERSION,
                    null,
                    null,
                    "+33999999999"
            );

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

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
            final Person existingPerson = PersonFactory.buildFromDatabase(
                    clientId,
                    "John Doe",
                    "john@example.com",
                    "+33111111111",
                    LocalDate.of(1990, 1, 1)
            );
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    VERSION,
                    null,
                    null,
                    null
            );

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingPerson.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo(existingPerson.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingPerson.getPhone().getValue());
            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingPerson);
            assertThat(((Person) result).getBirthDate()).isEqualTo(existingPerson.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN aucun champ WHEN execute on Company THEN ne change rien")
        void shouldNotChangeAnythingWhenNoFieldsProvidedForCompany() {
            // Given
            final UUID clientId = UUID.randomUUID();
            final Company existingCompany = CompanyFactory.buildFromDatabase(
                    clientId,
                    "Acme Corp",
                    "contact@acme.com",
                    "+33111111111",
                    "123456789"
            );
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    VERSION,
                    null,
                    null,
                    null
            );

            when(clientRepository.getById(clientId)).thenReturn(existingCompany);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo(existingCompany.getName().getValue());
            assertThat(result.getEmail().getValue()).isEqualTo(existingCompany.getEmail().getValue());
            assertThat(result.getPhone().getValue()).isEqualTo(existingCompany.getPhone().getValue());
            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingCompany);
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
        @DisplayName("GIVEN tous les champs WHEN execute THEN met à jour tous les champs")
        void shouldUpdateAllFieldsWhenAllProvided() {
            // Given
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    VERSION,
                    "Acme Corporation",
                    "info@acme.com",
                    "+33222222222"
            );

            when(clientRepository.getById(clientId)).thenReturn(existingCompany);

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
                    VERSION,
                    "Acme Corporation",
                    null,
                    null
            );

            when(clientRepository.getById(clientId)).thenReturn(existingCompany);

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
            PatchClient.Command command = new PatchClient.Command(
                    clientId,
                    VERSION,
                    "New Name",
                    null,
                    null
            );

            when(clientRepository.getById(clientId))
                    .thenThrow(ClientNotFoundException.forId(clientId));

            // When & Then
            assertThatThrownBy(() -> patchClientHandler.execute(command))
                    .isInstanceOf(ClientNotFoundException.class);
        }

        @Test
        @DisplayName("GIVEN même email que l'existant WHEN execute THEN conserve l'email")
        void shouldNotThrowWhenEmailIsIdenticalToExisting() {
            // Given
            String existingEmail = existingPerson.getEmail().getValue();
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, null, existingEmail, null);

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo(existingEmail);
        }

        @Test
        @DisplayName("GIVEN même téléphone que l'existant WHEN execute THEN conserve le téléphone")
        void shouldNotThrowWhenPhoneIsIdenticalToExisting() {
            // Given
            String existingPhone = existingPerson.getPhone().getValue();
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, null, null, existingPhone);

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            // When
            Client result = patchClientHandler.execute(command);

            // Then
            assertThat(result.getPhone().getValue()).isEqualTo(existingPhone);
        }

        @Test
        @DisplayName("GIVEN même téléphone dans un autre format WHEN execute THEN ne sauvegarde pas")
        void shouldNotSaveWhenSamePhoneInAnotherFormat() {
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, null, null, "+33 1 11 11 11 11");

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            Client result = patchClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result.getPhone()).isEqualTo(existingPerson.getPhone());
        }

        @Test
        @DisplayName("GIVEN même email dans une autre casse WHEN execute THEN ne sauvegarde pas")
        void shouldNotSaveWhenSameEmailInAnotherCase() {
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, null, "JOHN@Example.com", null);

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            Client result = patchClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result.getEmail()).isEqualTo(existingPerson.getEmail());
        }

        @Test
        @DisplayName("GIVEN même nom avec espaces WHEN execute THEN ne sauvegarde pas")
        void shouldNotSaveWhenSameNameWithSpaces() {
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, "  John Doe  ", null, null);

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            Client result = patchClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingPerson);
        }

        @Test
        @DisplayName("GIVEN tous les champs identiques WHEN execute THEN ne sauvegarde pas")
        void shouldNotSaveWhenAllFieldsAreIdentical() {
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, "John Doe", "john@example.com", "+33111111111");

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);

            Client result = patchClientHandler.execute(command);

            verify(clientRepository, never()).save(any(Client.class));
            assertThat(result).isSameAs(existingPerson);
        }

        @Test
        @DisplayName("GIVEN email identique et nom différent WHEN execute THEN sauvegarde une seule fois le client modifié")
        void shouldSaveOnceWhenOnlySomeFieldsChange() {
            PatchClient.Command command = new PatchClient.Command(clientId, VERSION, "Jane Doe", "john@example.com", null);

            when(clientRepository.getById(clientId)).thenReturn(existingPerson);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Client result = patchClientHandler.execute(command);

            verify(clientRepository).save(result);
            assertThat(result.getName().getValue()).isEqualTo("Jane Doe");
            assertThat(result.getEmail()).isEqualTo(existingPerson.getEmail());
        }
    }

    @Nested
    @DisplayName("Version attendue")
    class ExpectedVersion {

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
        @DisplayName("GIVEN une modification WHEN execute THEN vérifie la version attendue")
        void shouldCheckExpectedVersion() {
            when(clientRepository.getById(clientId)).thenReturn(existingPerson);
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            patchClientHandler.execute(new PatchClient.Command(clientId, VERSION, "Jane Doe", null, null));

            verify(clientRepository).ensureVersion(clientId, VERSION);
        }

        @Test
        @DisplayName("GIVEN une version périmée WHEN execute THEN lève VersionMismatchException sans sauvegarder")
        void shouldRejectStaleVersion() {
            when(clientRepository.getById(clientId)).thenReturn(existingPerson);
            doThrow(new VersionMismatchException(VERSION, VERSION + 1)).when(clientRepository).ensureVersion(clientId, VERSION);

            assertThatThrownBy(() -> patchClientHandler.execute(new PatchClient.Command(clientId, VERSION, "Jane Doe", null, null)))
                    .isInstanceOf(VersionMismatchException.class);
            verify(clientRepository, never()).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN une version périmée et un email invalide WHEN execute THEN la précondition est évaluée avant la validation")
        void shouldCheckVersionBeforeValidatingContent() {
            when(clientRepository.getById(clientId)).thenReturn(existingPerson);
            doThrow(new VersionMismatchException(VERSION, VERSION + 1)).when(clientRepository).ensureVersion(clientId, VERSION);

            assertThatThrownBy(() -> patchClientHandler.execute(new PatchClient.Command(clientId, VERSION, null, "not-an-email", null)))
                    .isInstanceOf(VersionMismatchException.class);
        }
    }
}
