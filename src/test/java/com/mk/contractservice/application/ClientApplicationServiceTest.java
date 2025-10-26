package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientApplicationService - Create Person")
class ClientApplicationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ClientApplicationService service;

    @Test
    @DisplayName("Should create person successfully when email does not exist")
    void shouldCreatePersonSuccessfully() {
        // Given
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName().value()).isEqualTo(name);
        assertThat(result.getEmail().value()).isEqualTo(email.toLowerCase());
        assertThat(result.getPhone().value()).isEqualTo(phone);
        assertThat(result.getBirthDate()).isEqualTo(birthDate);
        assertThat(result.getId()).isNotNull();

        verify(clientRepository).existsByEmail(email);
        verify(clientRepository).save(any(Person.class));
    }

    @Test
    @DisplayName("Should throw ClientAlreadyExistsException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.createPerson(name, email, phone, birthDate))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasMessageContaining("Client already exists")
                .hasMessageContaining(email);

        verify(clientRepository).existsByEmail(email);
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check email existence with raw string (performance optimization)")
    void shouldCheckEmailWithRawString() {
        // Given
        String name = "John Doe";
        String email = "JOHN.DOE@EXAMPLE.COM"; // Uppercase
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.createPerson(name, email, phone, birthDate))
                .isInstanceOf(ClientAlreadyExistsException.class);

        // Verify email check is done with raw string (before creating VO)
        verify(clientRepository).existsByEmail(email);
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return Person type after save (cast from Client)")
    void shouldReturnPersonType() {
        // Given
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result).isInstanceOf(Person.class);
        assertThat(result.getBirthDate()).isNotNull(); // Person-specific field
    }

    @Test
    @DisplayName("Should validate email uniqueness constraint")
    void shouldValidateEmailUniqueness() {
        // Given
        String name = "Jane Doe";
        String email = "JOHN@EXAMPLE.COM"; // Same email, different case
        String phone = "+33987654321";
        LocalDate birthDate = LocalDate.of(1992, 2, 2);

        when(clientRepository.existsByEmail(email)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.createPerson(name, email, phone, birthDate))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void shouldCallRepositoryMethodsInOrder() {
        // Given
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.createPerson(name, email, phone, birthDate);

        // Then - Verify order: check existence BEFORE save
        var inOrder = inOrder(clientRepository);
        inOrder.verify(clientRepository).existsByEmail(email);
        inOrder.verify(clientRepository).save(any(Person.class));
    }

    @Test
    @DisplayName("Should handle person with special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        String name = "Jean-François O'Connor";
        String email = "jf.oconnor@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1985, 5, 15);

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result.getName().value()).isEqualTo("Jean-François O'Connor");
    }

    @Test
    @DisplayName("Should handle person with minimum age (newborn)")
    void shouldHandleNewbornBirthDate() {
        // Given
        String name = "Baby Doe";
        String email = "baby@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.now(); // Born today

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.now());
        // Note: Business rule for minimum age could be added in service layer
    }

    @Test
    @DisplayName("Should handle person with very old birthDate")
    void shouldHandleVeryOldBirthDate() {
        // Given
        String name = "Old Person";
        String email = "old@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1900, 1, 1);

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1900, 1, 1));
    }

    @Test
    @DisplayName("Should create person with null ID before save (database will generate it)")
    void shouldCreatePersonWithNullIdBeforeSave() {
        // Given
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phone = "+33123456789";
        LocalDate birthDate = LocalDate.of(1990, 1, 15);

        UUID generatedId = UUID.randomUUID();

        when(clientRepository.existsByEmail(email)).thenReturn(false);
        when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            // Simulate database generating UUID
            return new Person(person.getName(), person.getEmail(), person.getPhone(), person.getBirthDate());
        });

        // When
        Person result = service.createPerson(name, email, phone, birthDate);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getId()).isEqualTo(generatedId);
    }
}

