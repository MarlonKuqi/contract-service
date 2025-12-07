package com.mk.contractservice.application;

import com.mk.contractservice.application.dto.ClientDto;
import com.mk.contractservice.application.dto.CompanyDto;
import com.mk.contractservice.application.dto.PersonDto;
import com.mk.contractservice.application.mapper.ClientMapper;
import com.mk.contractservice.application.service.ClientApplicationService;
import com.mk.contractservice.application.service.ContractApplicationService;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientService;
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

    @Mock
    private ClientMapper mapper;

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

            Person person = Person.of(
                    ClientName.of(name),
                    Email.of(email),
                    PhoneNumber.of(phone),
                    PersonBirthDate.of(birthDate)
            );

            PersonDto expectedDto = new PersonDto(
                    UUID.randomUUID(),
                    name,
                    email.toLowerCase(),
                    phone,
                    birthDate
            );

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson(name, email, phone, birthDate);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(name);
            assertThat(result.email()).isEqualTo(email.toLowerCase());
            assertThat(result.phone()).isEqualTo(phone);
            assertThat(result.birthDate()).isEqualTo(birthDate);

            verify(clientService).createAndPersistPerson(
                    any(ClientName.class),
                    any(Email.class),
                    any(PhoneNumber.class),
                    any(PersonBirthDate.class)
            );
            verify(mapper).toPersonDto(person);
        }

        @Test
        @DisplayName("Should reject duplicate email to ensure unique clients")
        void shouldRejectDuplicateEmail() {
            String email = "existing@example.com";

            doThrow(new ClientAlreadyExistsException("Client already exists", email))
                    .when(clientService).createAndPersistPerson(any(), any(), any(), any());

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

            Person person = Person.of(
                    ClientName.of("John"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(isoDate)
            );
            PersonDto expectedDto = new PersonDto(UUID.randomUUID(), "John", "john@example.com", "+33123456789", isoDate);

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson("John", "john@example.com", "+33123456789", isoDate);

            assertThat(result.birthDate()).isEqualTo(isoDate);
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

            PersonDto expectedDto = new PersonDto(
                    personId,
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));
            when(mapper.toDto(person)).thenReturn(expectedDto);

            ClientDto result = service.getClientById(personId);

            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto foundPerson = (PersonDto) result;
            assertThat(foundPerson.name()).isEqualTo("John Doe");
            assertThat(foundPerson.email()).isEqualTo("john@example.com");
            assertThat(foundPerson.phone()).isEqualTo("+33123456789");
            assertThat(foundPerson.birthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
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
        @DisplayName("GIVEN person exists WHEN read THEN return PersonDto type (not just ClientDto)")
        void shouldReturnCorrectType() {
            UUID personId = UUID.randomUUID();
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            PersonDto expectedDto = new PersonDto(
                    personId,
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientRepository.findById(personId)).thenReturn(Optional.of(person));
            when(mapper.toDto(person)).thenReturn(expectedDto);

            ClientDto result = service.getClientById(personId);

            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto foundPerson = (PersonDto) result;
            assertThat(foundPerson.birthDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("US3: Update Person")
    class UpdatePersonTests {

        @Test
        @DisplayName("GIVEN existing person WHEN update name, email, phone THEN changes are applied")
        void shouldUpdateAllowedFields() {
            UUID personId = UUID.randomUUID();
            Person updatedPerson = Person.reconstitute(
                    personId,
                    ClientName.of("Jane Doe"),
                    Email.of("jane@example.com"),
                    PhoneNumber.of("+33222222222"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            PersonDto expectedDto = new PersonDto(
                    personId,
                    "Jane Doe",
                    "jane@example.com",
                    "+33222222222",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientService.updateAndPersistCommonFields(any(), any(), any(), any())).thenReturn(updatedPerson);
            when(mapper.toDto(updatedPerson)).thenReturn(expectedDto);

            ClientDto result = service.updateCommonFields(personId, "Jane Doe", "jane@example.com", "+33222222222");

            // Verify the returned DTO has updated fields
            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto personDto = (PersonDto) result;
            assertThat(personDto.name()).isEqualTo("Jane Doe");
            assertThat(personDto.email()).isEqualTo("jane@example.com");
            assertThat(personDto.phone()).isEqualTo("+33222222222");

            verify(clientService).updateAndPersistCommonFields(any(), any(), any(), any());
            verify(mapper).toDto(updatedPerson);
        }

        @Test
        @DisplayName("GIVEN existing person WHEN update THEN birthdate MUST remain unchanged")
        void shouldNotUpdateBirthdate() {
            UUID personId = UUID.randomUUID();
            LocalDate originalBirthDate = LocalDate.of(1990, 5, 15);
            Person updatedPerson = Person.reconstitute(
                    personId,
                    ClientName.of("Updated Name"),
                    Email.of("updated@example.com"),
                    PhoneNumber.of("+33999999999"),
                    PersonBirthDate.of(originalBirthDate)
            );

            PersonDto expectedDto = new PersonDto(
                    personId,
                    "Updated Name",
                    "updated@example.com",
                    "+33999999999",
                    originalBirthDate
            );

            when(clientService.updateAndPersistCommonFields(any(), any(), any(), any())).thenReturn(updatedPerson);
            when(mapper.toDto(updatedPerson)).thenReturn(expectedDto);

            ClientDto result = service.updateCommonFields(
                    personId,
                    "Updated Name",
                    "updated@example.com",
                    "+33999999999"
            );

            // Verify birthdate remains unchanged in the DTO
            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto personDto = (PersonDto) result;
            assertThat(personDto.birthDate())
                    .as("Birthdate is immutable as per subject requirement")
                    .isEqualTo(originalBirthDate);
        }

        @Test
        @DisplayName("GIVEN non-existent person WHEN update THEN throw ClientNotFoundException")
        void shouldThrowExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            doThrow(new ClientNotFoundException("Client with ID " + nonExistentId + " not found"))
                    .when(clientService).updateAndPersistCommonFields(any(), any(), any(), any());

            assertThatThrownBy(() -> service.updateCommonFields(
                    nonExistentId,
                    "Name",
                    "email@example.com",
                    "+33111111111"
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

            Company company = Company.of(
                    ClientName.of(name),
                    Email.of(email),
                    PhoneNumber.of(phone),
                    CompanyIdentifier.of(companyId)
            );

            CompanyDto expectedDto = new CompanyDto(
                    UUID.randomUUID(),
                    name,
                    email.toLowerCase(),
                    phone,
                    companyId
            );

            when(clientService.createAndPersistCompany(any(), any(), any(), any())).thenReturn(company);
            when(mapper.toCompanyDto(company)).thenReturn(expectedDto);

            CompanyDto result = service.createCompany(name, email, phone, companyId);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(name);
            assertThat(result.email()).isEqualTo(email.toLowerCase());
            assertThat(result.phone()).isEqualTo(phone);
            assertThat(result.companyIdentifier()).isEqualTo(companyId);

            verify(clientService).createAndPersistCompany(
                    any(ClientName.class),
                    any(Email.class),
                    any(PhoneNumber.class),
                    any(CompanyIdentifier.class)
            );
            verify(mapper).toCompanyDto(company);
        }
    }

    @Nested
    @DisplayName("Patch Client")
    class PatchClientTests {

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            UUID clientId = UUID.randomUUID();
            Person updatedPerson = Person.reconstitute(
                    clientId,
                    ClientName.of("Jane Doe"),
                    Email.of("john@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            PersonDto expectedDto = new PersonDto(
                    clientId,
                    "Jane Doe",
                    "john@example.com",
                    "+33111111111",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientService.patchAndPersistClient(any(), any(), any(), any())).thenReturn(updatedPerson);
            when(mapper.toDto(updatedPerson)).thenReturn(expectedDto);

            ClientDto result = service.patchClient(clientId, "Jane Doe", null, null);

            // Vérifier le DTO retourné
            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto personDto = (PersonDto) result;
            assertThat(personDto.name()).isEqualTo("Jane Doe");
            assertThat(personDto.email()).isEqualTo("john@example.com");
            assertThat(personDto.phone()).isEqualTo("+33111111111");

            verify(clientService).patchAndPersistClient(any(), any(), any(), any());
            verify(mapper).toDto(updatedPerson);
        }

        @Test
        @DisplayName("Should update all provided fields")
        void shouldUpdateAllProvidedFields() {
            UUID clientId = UUID.randomUUID();
            Person updatedPerson = Person.reconstitute(
                    clientId,
                    ClientName.of("Jane Smith"),
                    Email.of("jane@example.com"),
                    PhoneNumber.of("+33999999999"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            PersonDto expectedDto = new PersonDto(
                    clientId,
                    "Jane Smith",
                    "jane@example.com",
                    "+33999999999",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientService.patchAndPersistClient(any(), any(), any(), any())).thenReturn(updatedPerson);
            when(mapper.toDto(updatedPerson)).thenReturn(expectedDto);

            ClientDto result = service.patchClient(clientId, "Jane Smith", "jane@example.com", "+33999999999");

            // Vérifier la nouvelle instance retournée
            assertThat(result).isInstanceOf(PersonDto.class);
            PersonDto personDto = (PersonDto) result;
            assertThat(personDto.name()).isEqualTo("Jane Smith");
            assertThat(personDto.email()).isEqualTo("jane@example.com");
            assertThat(personDto.phone()).isEqualTo("+33999999999");

            verify(clientService).patchAndPersistClient(any(), any(), any(), any());
            verify(mapper).toDto(updatedPerson);
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
            PersonDto expectedDto = new PersonDto(UUID.randomUUID(), name, "jf@example.com", "+33123456789", LocalDate.of(1985, 5, 15));

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson(name, "jf@example.com", "+33123456789", LocalDate.of(1985, 5, 15));

            assertThat(result.name()).isEqualTo(name);
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
            PersonDto expectedDto = new PersonDto(UUID.randomUUID(), "Baby", "baby@example.com", "+33123456789", today);

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson("Baby", "baby@example.com", "+33123456789", today);

            assertThat(result.birthDate()).isEqualTo(today);
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
            PersonDto expectedDto = new PersonDto(UUID.randomUUID(), "Old", "old@example.com", "+33123456789", oldDate);

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson("Old", "old@example.com", "+33123456789", oldDate);

            assertThat(result.birthDate()).isEqualTo(oldDate);
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
            PersonDto expectedDto = new PersonDto(UUID.randomUUID(), "John", "john.doe@example.com", "+33123456789", LocalDate.of(1990, 1, 1));

            when(clientService.createAndPersistPerson(any(), any(), any(), any())).thenReturn(person);
            when(mapper.toPersonDto(person)).thenReturn(expectedDto);

            PersonDto result = service.createPerson("John", mixedCaseEmail, "+33123456789", LocalDate.of(1990, 1, 1));

            assertThat(result.email()).isEqualTo("john.doe@example.com");
        }
    }
}

