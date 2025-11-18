package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientService;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
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
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private ClientService clientService;

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

            when(clientService.createPerson(any(), any(), any(), any()))
                    .thenAnswer(invocation -> Person.of(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3)));
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson(name, email, phone, birthDate);

            assertThat(result).isNotNull();
            assertThat(result.getName().value()).isEqualTo(name);
            assertThat(result.getEmail().value()).isEqualTo(email.toLowerCase());
            assertThat(result.getPhone().value()).isEqualTo(phone);
            assertThat(result.getBirthDate().value()).isEqualTo(birthDate);

            verify(clientService).createPerson(
                    any(ClientName.class),
                    any(Email.class),
                    any(PhoneNumber.class),
                    any(PersonBirthDate.class)
            );
            verify(clientRepository).save(any(Person.class));
        }

        @Test
        @DisplayName("Should reject duplicate email to ensure unique clients")
        void shouldRejectDuplicateEmail() {
            String email = "existing@example.com";

            doThrow(new ClientAlreadyExistsException("Client already exists", email))
                    .when(clientService).createPerson(any(), any(), any(), any());

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

            when(clientService.createPerson(any(), any(), any(), any()))
                    .thenAnswer(invocation -> Person.of(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3)
                    ));
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
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

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
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

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
            Person existingPerson = Person.reconstitute(
                    personId,
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ClientName newName = ClientName.of("Jane Doe");
            Email newEmail = Email.of("jane@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+33222222222");

            Client result = service.updateCommonFields(personId, newName, newEmail, newPhone);

            // Verify the returned instance has updated fields
            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getEmail()).isEqualTo(newEmail);
            assertThat(result.getPhone()).isEqualTo(newPhone);

            // Verify save was called
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("GIVEN existing person WHEN update THEN birthdate MUST remain unchanged")
        void shouldNotUpdateBirthdate() {
            UUID personId = UUID.randomUUID();
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
            Person existingPerson = Person.reconstitute(
                    personId,
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    originalBirthDate
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(existingPerson));
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Client result = service.updateCommonFields(
                    personId,
                    ClientName.of("Updated Name"),
                    Email.of("updated@example.com"),
                    PhoneNumber.of("+33999999999")
            );

            // Verify birthdate remains unchanged in the new instance
            assertThat(((Person) result).getBirthDate())
                    .as("Birthdate is immutable as per subject requirement")
                    .isEqualTo(originalBirthDate);
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
    @DisplayName("Create Company")
    class CreateCompanyTests {

        @Test
        @DisplayName("Should create company with all required fields")
        void shouldCreateCompany() {
            String name = "Acme Corp";
            String email = "contact@acme.com";
            String phone = "+33123456789";
            String companyId = "CHE-123.456.789";

            when(clientService.createCompany(any(), any(), any(), any()))
                    .thenAnswer(invocation -> Company.of(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3)
                    ));
            when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Company result = service.createCompany(name, email, phone, companyId);

            assertThat(result).isNotNull();
            assertThat(result.getName().value()).isEqualTo(name);
            assertThat(result.getEmail().value()).isEqualTo(email.toLowerCase());
            assertThat(result.getPhone().value()).isEqualTo(phone);
            assertThat(result.getCompanyIdentifier().value()).isEqualTo(companyId);

            verify(clientService).createCompany(
                    any(ClientName.class),
                    any(Email.class),
                    any(PhoneNumber.class),
                    any(CompanyIdentifier.class)
            );
            verify(clientRepository).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("Patch Client")
    class PatchClientTests {

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            UUID clientId = UUID.randomUUID();
            Person existingPerson = Person.reconstitute(
                    clientId,
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingPerson));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ClientName newName = ClientName.of("Jane Doe");
            Client result = service.patchClient(clientId, newName, null, null);

            // Vérifier la nouvelle instance retournée
            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getEmail().value()).isEqualTo("john@example.com");
            assertThat(result.getPhone().value()).isEqualTo("+33111111111");

            // Vérifier que l'originale n'a pas changé (immutabilité)
            assertThat(existingPerson.getName().value()).isEqualTo("John Doe");

            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("Should not save when no fields provided")
        void shouldNotSaveWhenNoChanges() {
            UUID clientId = UUID.randomUUID();
            Person existingPerson = Person.of(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingPerson));

            service.patchClient(clientId, null, null, null);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update all provided fields")
        void shouldUpdateAllProvidedFields() {
            UUID clientId = UUID.randomUUID();
            Person existingPerson = Person.reconstitute(
                    clientId,
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingPerson));
            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ClientName newName = ClientName.of("Jane Smith");
            Email newEmail = Email.of("jane@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+33999999999");

            Client result = service.patchClient(clientId, newName, newEmail, newPhone);

            // Vérifier la nouvelle instance retournée
            assertThat(result.getName()).isEqualTo(newName);
            assertThat(result.getEmail()).isEqualTo(newEmail);
            assertThat(result.getPhone()).isEqualTo(newPhone);

            // Vérifier que l'originale n'a pas changé (immutabilité)
            assertThat(existingPerson.getName().value()).isEqualTo("John Doe");

            verify(clientRepository).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases & Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("GIVEN name with special characters WHEN create THEN accept valid characters")
        void shouldAcceptSpecialCharactersInName() {
            String name = "Jean-François O'Connor";

            Person person = Person.of(
                    ClientName.of(name),
                    Email.of("jf@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1985, 5, 15))
            );

            when(clientService.createPerson(any(), any(), any(), any())).thenReturn(person);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson(name, "jf@example.com", "+33123456789", LocalDate.of(1985, 5, 15));

            assertThat(result.getName().value()).isEqualTo(name);
        }

        @Test
        @DisplayName("GIVEN birthdate today WHEN create THEN accept (newborn)")
        void shouldAcceptTodayBirthdate() {
            LocalDate today = LocalDate.now();

            Person person = Person.of(
                    ClientName.of("Baby"),
                    Email.of("baby@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(today)
            );

            when(clientService.createPerson(any(), any(), any(), any())).thenReturn(person);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("Baby", "baby@example.com", "+33123456789", today);

            assertThat(result.getBirthDate().value()).isEqualTo(today);
        }

        @Test
        @DisplayName("GIVEN very old birthdate WHEN create THEN accept (historical data)")
        void shouldAcceptOldBirthdate() {
            LocalDate oldDate = LocalDate.of(1900, 1, 1);

            Person person = Person.of(
                    ClientName.of("Old"),
                    Email.of("old@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(oldDate)
            );

            when(clientService.createPerson(any(), any(), any(), any())).thenReturn(person);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("Old", "old@example.com", "+33123456789", oldDate);

            assertThat(result.getBirthDate().value()).isEqualTo(oldDate);
        }

        @Test
        @DisplayName("GIVEN email with uppercase WHEN create THEN normalize to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String mixedCaseEmail = "John.DOE@Example.COM";

            Person person = Person.of(
                    ClientName.of("John"),
                    Email.of(mixedCaseEmail),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            when(clientService.createPerson(any(), any(), any(), any())).thenReturn(person);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createPerson("John", mixedCaseEmail, "+33123456789", LocalDate.of(1990, 1, 1));

            assertThat(result.getEmail().value()).isEqualTo("john.doe@example.com");
        }
    }
}

