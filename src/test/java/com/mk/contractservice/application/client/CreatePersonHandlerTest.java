package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.create.core.CreatePerson;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePersonUseCase - Unit Tests")
class CreatePersonHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientValidationService clientValidationService;

    @InjectMocks
    private CreatePerson.Handler createPerson;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should create and save person")
        void shouldCreateAndSavePerson() {
            // Given
            String name = "John Doe";
            String email = "john.doe@example.com";
            String phone = "+33123456789";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            CreatePerson.Command command = new CreatePerson.Command(
                    name,
                    email,
                    phone,
                    birthDate
            );

            Person expectedPerson = Person.of(
                    ClientName.of(name),
                    ClientEmail.of(email),
                    ClientPhoneNumber.of(phone),
                    PersonBirthDate.of(birthDate)
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(expectedPerson);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName().getValue()).isEqualTo(name);
            assertThat(result.getEmail().getValue()).isEqualTo(email.toLowerCase());
            assertThat(result.getPhone().getValue()).isEqualTo(phone);
            assertThat(result.getBirthDate().getValue()).isEqualTo(birthDate);

            verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            verify(clientRepository).save(any(Person.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should validate email uniqueness")
        void shouldValidateEmailUniqueness() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "Jane Smith",
                    "jane.smith@example.com",
                    "+33987654321",
                    LocalDate.of(1985, 3, 20)
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            createPerson.execute(command);

            // Then
            ArgumentCaptor<ClientEmail> emailCaptor = ArgumentCaptor.forClass(ClientEmail.class);
            verify(clientValidationService).ensureEmailIsUnique(emailCaptor.capture());
            assertThat(emailCaptor.getValue().getValue()).isEqualTo(command.email().toLowerCase());
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should validate before saving")
        void shouldValidateBeforeSaving() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    "john.doe@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            createPerson.execute(command);

            // Then
            var ordered = inOrder(clientValidationService, clientRepository);
            ordered.verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            ordered.verify(clientRepository).save(any(Person.class));
        }

        @Test
        @DisplayName("GIVEN person with different birth dates WHEN execute THEN should create person with correct birth date")
        void shouldCreatePersonWithCorrectBirthDate() {
            // Given
            LocalDate birthDate = LocalDate.of(1995, 12, 25);
            CreatePerson.Command command = new CreatePerson.Command(
                    "Alice Wonder",
                    "alice@example.com",
                    "+33111222333",
                    birthDate
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getBirthDate().getValue()).isEqualTo(birthDate);
        }
    }

    @Nested
    @DisplayName("execute() - Validation Errors")
    class ExecuteValidationErrors {

        @Test
        @DisplayName("GIVEN duplicate email WHEN execute THEN should throw ClientAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "existing@example.com";
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    duplicateEmail,
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            doThrow(new ClientAlreadyExistsException("Email already exists: " + duplicateEmail))
                    .when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));

            // When & Then
            assertThatThrownBy(() -> createPerson.execute(command))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining(duplicateEmail);

            verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            verify(clientRepository, never()).save(any(Person.class));
        }

        @Test
        @DisplayName("GIVEN duplicate email WHEN execute THEN should not save person")
        void shouldNotSavePersonWhenEmailExists() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    "existing@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            doThrow(new ClientAlreadyExistsException("Email already exists"))
                    .when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));

            // When & Then
            try {
                createPerson.execute(command);
            } catch (ClientAlreadyExistsException e) {
                // Expected exception
            }

            verify(clientRepository, never()).save(any(Person.class));
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN email with uppercase WHEN execute THEN should normalize to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    "John.Doe@EXAMPLE.COM",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("GIVEN person name with special characters WHEN execute THEN should create person")
        void shouldCreatePersonWithSpecialCharactersInName() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "Jean-Pierre O'Connor",
                    "jean@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Jean-Pierre O'Connor");
        }

        @Test
        @DisplayName("GIVEN very young person WHEN execute THEN should create person")
        void shouldCreateYoungPerson() {
            // Given
            LocalDate recentBirthDate = LocalDate.now().minusYears(18);
            CreatePerson.Command command = new CreatePerson.Command(
                    "Young Person",
                    "young@example.com",
                    "+33123456789",
                    recentBirthDate
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getBirthDate().getValue()).isEqualTo(recentBirthDate);
        }

        @Test
        @DisplayName("GIVEN elderly person WHEN execute THEN should create person")
        void shouldCreateElderlyPerson() {
            // Given
            LocalDate oldBirthDate = LocalDate.of(1930, 1, 1);
            CreatePerson.Command command = new CreatePerson.Command(
                    "Elderly Person",
                    "elderly@example.com",
                    "+33123456789",
                    oldBirthDate
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getBirthDate().getValue()).isEqualTo(oldBirthDate);
        }

        @Test
        @DisplayName("GIVEN international phone number WHEN execute THEN should create person")
        void shouldCreatePersonWithInternationalPhone() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    "john@example.com",
                    "+441234567890",
                    LocalDate.of(1990, 5, 15)
            );

            Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            when(clientRepository.save(any(Person.class))).thenReturn(person);

            // When
            Person result = createPerson.execute(command);

            // Then
            assertThat(result.getPhone().getValue()).isEqualTo("+441234567890");
        }
    }
}

