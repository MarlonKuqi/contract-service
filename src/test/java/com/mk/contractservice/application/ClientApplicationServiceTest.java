package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
    @DisplayName("US1: Create Person - Business Behavior")
    class CreatePersonTests {

        @Test
        @DisplayName("Should create person with all required fields when data is valid")
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
        }

        @Test
        @DisplayName("Should reject duplicate email to ensure unique clients")
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
        @DisplayName("Should accept ISO 8601 date format as per specification")
        void shouldAcceptISO8601DateFormat() {
            LocalDate isoDate = LocalDate.parse("1990-05-15");

            when(clientRepository.existsByEmail(any(String.class))).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("John", "john@example.com", "+33123456789", isoDate);

            assertThat(result.getBirthDate().value()).isEqualTo(isoDate);
        }
    }

    @Nested
    @DisplayName("US2: Read Person")
    class ReadPersonTests {

        @Test
        @DisplayName("GIVEN existing person WHEN findById THEN return person with ALL fields")
        void shouldReturnPersonWithAllFields() {
            UUID personId = UUID.randomUUID();
            Person person = Person.builder()
                    .name(ClientName.of("John Doe"))
                    .email(Email.of("john@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                    .build();

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));

            Client result = service.getClientById(personId);

            Person foundPerson = (Person) result;
            assertThat(foundPerson.getName()).isEqualTo(person.getName());
            assertThat(foundPerson.getEmail()).isEqualTo(person.getEmail());
            assertThat(foundPerson.getPhone()).isEqualTo(person.getPhone());
            assertThat(foundPerson.getBirthDate()).isEqualTo(person.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN getClientById THEN throw ClientNotFoundException")
        void shouldThrowExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClientById(nonExistentId))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("Client with ID " + nonExistentId + " not found");
        }

        @Test
        @DisplayName("GIVEN person exists WHEN read THEN return Person type (not just Client)")
        void shouldReturnCorrectType() {
            UUID personId = UUID.randomUUID();
            Person person = Person.builder()
                    .name(ClientName.of("John Doe"))
                    .email(Email.of("john@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                    .build();

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));

            Client result = service.getClientById(personId);

            assertThat(result).isInstanceOf(Person.class);
            Person foundPerson = (Person) result;
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
            Person existingPerson = Person.builder()
                    .name(ClientName.of("John Doe"))
                    .email(Email.of("john@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                    .build();

            when(clientRepository.findById(personId)).thenReturn(Optional.of(existingPerson));

            ClientName newName = ClientName.of("Jane Doe");
            Email newEmail = Email.of("jane@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+33222222222");

            service.updateCommonFields(personId, newName, newEmail, newPhone);

            assertThat(existingPerson.getName()).isEqualTo(newName);
            assertThat(existingPerson.getEmail()).isEqualTo(newEmail);
            assertThat(existingPerson.getPhone()).isEqualTo(newPhone);
        }

        @Test
        @DisplayName("GIVEN existing person WHEN update THEN birthdate MUST remain unchanged")
        void shouldNotUpdateBirthdate() {
            UUID personId = UUID.randomUUID();
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
            Person existingPerson = Person.builder()
                    .name(ClientName.of("John Doe"))
                    .email(Email.of("john@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(originalBirthDate)
                    .build();

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
        @DisplayName("GIVEN non-existent person WHEN update THEN throw ClientNotFoundException")
        void shouldThrowExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCommonFields(
                    nonExistentId,
                    ClientName.of("Name"),
                    Email.of("email@example.com"),
                    PhoneNumber.of("+33111111111")
            ))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("Client with ID " + nonExistentId + " not found");
        }
    }

    @Nested
    @DisplayName("US4: Delete Person")
    class DeletePersonTests {

        @Test
        @DisplayName("GIVEN existing person WHEN delete THEN deletion succeeds (contracts closure is delegated)")
        void shouldDeleteExistingClient() {
            UUID personId = UUID.randomUUID();

            when(clientRepository.existsById(personId)).thenReturn(true);
            doNothing().when(contractApplicationService).closeActiveContractsByClientId(personId);
            doNothing().when(clientRepository).deleteById(personId);

            service.deleteClientAndCloseContracts(personId);

            verify(contractApplicationService).closeActiveContractsByClientId(personId);
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN delete THEN throw ClientNotFoundException and do nothing")
        void shouldThrowExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.existsById(nonExistentId)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteClientAndCloseContracts(nonExistentId))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("Client with ID " + nonExistentId + " not found");

            verify(contractApplicationService, never()).closeActiveContractsByClientId(any());
        }

        @Test
        @DisplayName("GIVEN person WHEN delete THEN contracts closure is delegated (business rule: close before delete)")
        void shouldDelegateContractsClosureBeforeDeletion() {
            UUID personId = UUID.randomUUID();

            when(clientRepository.existsById(personId)).thenReturn(true);
            doNothing().when(contractApplicationService).closeActiveContractsByClientId(personId);
            doNothing().when(clientRepository).deleteById(personId);

            service.deleteClientAndCloseContracts(personId);

            verify(contractApplicationService).closeActiveContractsByClientId(personId);
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

