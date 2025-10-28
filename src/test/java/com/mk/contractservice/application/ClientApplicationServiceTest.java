package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("Person Management Tests")
class ClientApplicationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractApplicationService contractApplicationService;

    @InjectMocks
    private ClientApplicationService service;

    @Nested
    @DisplayName("US1: Create Person")
    class CreatePersonTests {

        @Test
        @DisplayName("GIVEN valid person data WHEN create THEN person is created with all required fields")
        void shouldCreatePersonWithAllRequiredFields() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String phone = "+33123456789";
            LocalDate birthDate = LocalDate.of(1990, 1, 15);

            when(clientRepository.existsByEmail(email)).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson(name, email, phone, birthDate);

            assertThat(result).isNotNull();
            assertThat(result.getName().value()).isEqualTo(name);
            assertThat(result.getEmail().value()).isEqualTo(email.toLowerCase());
            assertThat(result.getPhone().value()).isEqualTo(phone);
            assertThat(result.getBirthDate().value()).isEqualTo(birthDate);

            verify(clientRepository).save(any(Person.class));
        }

        @Test
        @DisplayName("GIVEN duplicate email WHEN create THEN fail with ClientAlreadyExistsException")
        void shouldRejectDuplicateEmail() {
            String email = "existing@example.com";

            when(clientRepository.existsByEmail(email)).thenReturn(true);

            assertThatThrownBy(() -> service.createPerson("John", email, "+33123456789", LocalDate.of(1990, 1, 1)))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining("Client already exists")
                    .extracting("businessKey")
                    .isEqualTo(email);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN invalid email format WHEN create THEN fail with IllegalArgumentException")
        void shouldValidateEmailFormat() {
            String invalidEmail = "not-an-email";

            assertThatThrownBy(() -> service.createPerson("John", invalidEmail, "+33123456789", LocalDate.of(1990, 1, 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("GIVEN invalid phone format WHEN create THEN fail with IllegalArgumentException")
        void shouldValidatePhoneFormat() {
            String invalidPhone = "123";

            assertThatThrownBy(() -> service.createPerson("John", "john@example.com", invalidPhone, LocalDate.of(1990, 1, 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("GIVEN ISO 8601 date WHEN create THEN date is correctly stored")
        void shouldAcceptISO8601DateFormat() {
            LocalDate isoDate = LocalDate.parse("1990-05-15");

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("John", "john@example.com", "+33123456789", isoDate);

            assertThat(result.getBirthDate().value()).isEqualTo(isoDate);
        }

        @Test
        @DisplayName("GIVEN null birthdate WHEN create THEN fail with domain validation error")
        void shouldRejectNullBirthdate() {
            assertThatThrownBy(() -> service.createPerson("John", "john@example.com", "+33123456789", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth date must not be null");
        }

        @Test
        @DisplayName("GIVEN future birthdate WHEN create THEN fail with domain validation error")
        void shouldRejectFutureBirthdate() {
            LocalDate futureDate = LocalDate.now().plusYears(1);

            assertThatThrownBy(() -> service.createPerson("John", "john@example.com", "+33123456789", futureDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth date cannot be in the future");
        }

        @Test
        @DisplayName("GIVEN empty name WHEN create THEN fail with validation error")
        void shouldValidateNameNotEmpty() {
            String emptyName = "";

            assertThatThrownBy(() -> service.createPerson(emptyName, "john@example.com", "+33123456789", LocalDate.of(1990, 1, 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("US2: Read Person")
    class ReadPersonTests {

        @Test
        @DisplayName("GIVEN existing person WHEN findById THEN return person with ALL fields")
        void shouldReturnPersonWithAllFields() {
            UUID personId = UUID.randomUUID();
            Person person = new Person(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));

            Optional<Client> result = service.findById(personId);

            assertThat(result).isPresent();
            Person foundPerson = (Person) result.get();
            assertThat(foundPerson.getName()).isEqualTo(person.getName());
            assertThat(foundPerson.getEmail()).isEqualTo(person.getEmail());
            assertThat(foundPerson.getPhone()).isEqualTo(person.getPhone());
            assertThat(foundPerson.getBirthDate()).isEqualTo(person.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN findById THEN return empty")
        void shouldReturnEmptyWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            Optional<Client> result = service.findById(nonExistentId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("GIVEN person exists WHEN read THEN return Person type (not just Client)")
        void shouldReturnCorrectType() {
            UUID personId = UUID.randomUUID();
            Person person = new Person(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));

            Optional<Client> result = service.findById(personId);

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(Person.class);
            Person foundPerson = (Person) result.get();
            assertThat(foundPerson.getBirthDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("US3: Update Person")
    class UpdatePersonTests {

        @Test
        @DisplayName("GIVEN existing person WHEN update name, email, phone THEN changes are applied")
        void shouldUpdateAllowedFields() {
            UUID personId = UUID.randomUUID();
            Person existingPerson = new Person(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(existingPerson));

            ClientName newName = ClientName.of("Jane Doe");
            Email newEmail = Email.of("jane@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+33222222222");

            boolean updated = service.updateCommonFields(personId, newName, newEmail, newPhone);

            assertThat(updated).isTrue();
            assertThat(existingPerson.getName()).isEqualTo(newName);
            assertThat(existingPerson.getEmail()).isEqualTo(newEmail);
            assertThat(existingPerson.getPhone()).isEqualTo(newPhone);
        }

        @Test
        @DisplayName("GIVEN existing person WHEN update THEN birthdate MUST remain unchanged")
        void shouldNotUpdateBirthdate() {
            UUID personId = UUID.randomUUID();
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
            Person existingPerson = new Person(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    originalBirthDate
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(existingPerson));

            service.updateCommonFields(
                    personId,
                    ClientName.of("Updated Name"),
                    Email.of("updated@example.com"),
                    PhoneNumber.of("+33999999999")
            );

            assertThat(existingPerson.getBirthDate())
                    .isEqualTo(originalBirthDate)
                    .as("Birthdate is immutable as per subject requirement");
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN update THEN return false")
        void shouldReturnFalseWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            boolean updated = service.updateCommonFields(
                    nonExistentId,
                    ClientName.of("Name"),
                    Email.of("email@example.com"),
                    PhoneNumber.of("+33111111111")
            );

            assertThat(updated).isFalse();
        }

        @Test
        @DisplayName("GIVEN existing person WHEN update with invalid email THEN fail validation")
        void shouldValidateEmailOnUpdate() {
            UUID personId = UUID.randomUUID();

            assertThatThrownBy(() -> service.updateCommonFields(
                    personId,
                    ClientName.of("John"),
                    Email.of("invalid-email"),
                    PhoneNumber.of("+33111111111")
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * USER STORY 4 (Subject):
     * "Delete a client: When a client is deleted the end date of their contracts
     * should be updated to the current date"
     * <p>
     * TDD APPROACH: Ce test capture la règle métier la plus critique de la suppression
     */
    @Nested
    @DisplayName("US4: Delete Person")
    class DeletePersonTests {

        @Test
        @DisplayName("GIVEN person with contracts WHEN delete THEN contracts are closed with current date")
        void shouldCloseContractsWhenDeleted() {
            UUID personId = UUID.randomUUID();

            when(clientRepository.existsById(personId)).thenReturn(true);
            doNothing().when(contractApplicationService).closeActiveContractsByClientId(personId);
            doNothing().when(clientRepository).deleteById(personId);

            boolean deleted = service.deleteClientAndCloseContracts(personId);

            assertThat(deleted).isTrue();

            verify(contractApplicationService).closeActiveContractsByClientId(personId);
            verify(clientRepository).deleteById(personId);
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN delete THEN return false and do nothing")
        void shouldReturnFalseWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.existsById(nonExistentId)).thenReturn(false);

            boolean deleted = service.deleteClientAndCloseContracts(nonExistentId);

            assertThat(deleted).isFalse();
            verify(contractApplicationService, never()).closeActiveContractsByClientId(any());
            verify(clientRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("GIVEN person without contracts WHEN delete THEN deletion still succeeds")
        void shouldDeletePersonWithoutContracts() {
            UUID personId = UUID.randomUUID();

            when(clientRepository.existsById(personId)).thenReturn(true);
            doNothing().when(contractApplicationService).closeActiveContractsByClientId(personId);
            doNothing().when(clientRepository).deleteById(personId);

            boolean deleted = service.deleteClientAndCloseContracts(personId);

            assertThat(deleted).isTrue();
            verify(contractApplicationService).closeActiveContractsByClientId(personId);
            verify(clientRepository).deleteById(personId);
        }

        @Test
        @DisplayName("GIVEN person WHEN delete THEN contracts are closed BEFORE client deletion (order matters)")
        void shouldCloseContractsBeforeDeleting() {
            UUID personId = UUID.randomUUID();

            when(clientRepository.existsById(personId)).thenReturn(true);
            doNothing().when(contractApplicationService).closeActiveContractsByClientId(personId);
            doNothing().when(clientRepository).deleteById(personId);

            service.deleteClientAndCloseContracts(personId);

            var inOrder = inOrder(contractApplicationService, clientRepository);
            inOrder.verify(contractApplicationService).closeActiveContractsByClientId(personId);
            inOrder.verify(clientRepository).deleteById(personId);
        }
    }

    @Nested
    @DisplayName("Edge Cases & Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("GIVEN name with special characters WHEN create THEN accept valid characters")
        void shouldAcceptSpecialCharactersInName() {
            String name = "Jean-François O'Connor";

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson(name, "jf@example.com", "+33123456789", LocalDate.of(1985, 5, 15));

            assertThat(result.getName().value()).isEqualTo(name);
        }

        @Test
        @DisplayName("GIVEN birthdate today WHEN create THEN accept (newborn)")
        void shouldAcceptTodayBirthdate() {
            LocalDate today = LocalDate.now();

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("Baby", "baby@example.com", "+33123456789", today);

            assertThat(result.getBirthDate().value()).isEqualTo(today);
        }

        @Test
        @DisplayName("GIVEN very old birthdate WHEN create THEN accept (historical data)")
        void shouldAcceptOldBirthdate() {
            LocalDate oldDate = LocalDate.of(1900, 1, 1);

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("Old", "old@example.com", "+33123456789", oldDate);

            assertThat(result.getBirthDate().value()).isEqualTo(oldDate);
        }

        @Test
        @DisplayName("GIVEN email with uppercase WHEN create THEN normalize to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String mixedCaseEmail = "John.DOE@Example.COM";

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("John", mixedCaseEmail, "+33123456789", LocalDate.of(1990, 1, 1));

            assertThat(result.getEmail().value()).isEqualTo("john.doe@example.com");
        }
    }
}

