package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("PersonResponseMapper - Person to DTO")
class PersonResponseMapperTest {

    @Autowired
    private PersonResponseMapper mapper;

    @Test
    @DisplayName("Should map Person entity to CreatePersonResponse DTO")
    void shouldMapEntityToResponse() {
        // Given
        UUID personId = UUID.randomUUID();
        Person person = new Person(
                ClientName.of("John Doe"),
                Email.of("john.doe@example.com"),
                PhoneNumber.of("+33123456789"),
                LocalDate.of(1990, 1, 15)
        );

        // When
        CreatePersonResponse response = mapper.toDto(person);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(personId);
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.phone()).isEqualTo("+33123456789");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 15));
    }

    @Test
    @DisplayName("Should extract value() from Value Objects")
    void shouldExtractValuesFromValueObjects() {
        // Given
        Person person = new Person(
                ClientName.of("Jane Smith"),
                Email.of("JANE@DOMAIN.COM"), // Will be normalized
                PhoneNumber.of("+44 20 1234 5678"),
                LocalDate.of(1985, 5, 20)
        );

        // When
        CreatePersonResponse response = mapper.toDto(person);

        // Then
        assertThat(response.name()).isEqualTo("Jane Smith");
        assertThat(response.email()).isEqualTo("jane@domain.com"); // Normalized
        assertThat(response.phone()).isEqualTo("+44 20 1234 5678");
    }

    @Test
    @DisplayName("Should handle birthDate correctly")
    void shouldHandleBirthDate() {
        // Given
        LocalDate birthDate = LocalDate.of(2000, 12, 31);
        Person person = new Person(
                ClientName.of("Bob Builder"),
                Email.of("bob@example.com"),
                PhoneNumber.of("+1234567890"),
                birthDate
        );

        // When
        CreatePersonResponse response = mapper.toDto(person);

        // Then
        assertThat(response.birthDate()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("Should preserve UUID in response")
    void shouldPreserveUuid() {
        // Given
        UUID specificId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Person person = new Person(
                ClientName.of("Charlie Brown"),
                Email.of("charlie@example.com"),
                PhoneNumber.of("+9876543210"),
                LocalDate.of(1988, 8, 8)
        );

        // When
        CreatePersonResponse response = mapper.toDto(person);

        // Then
        assertThat(response.id()).isEqualTo(specificId);
    }

    @Test
    @DisplayName("Should create DTO with all required fields")
    void shouldCreateDtoWithAllFields() {
        // Given
        Person person = new Person(
                ClientName.of("Alice Wonder"),
                Email.of("alice@wonderland.com"),
                PhoneNumber.of("+33 1 11 11 11 11"),
                LocalDate.of(1995, 3, 10)
        );

        // When
        CreatePersonResponse response = mapper.toDto(person);

        // Then
        assertThat(response.id()).isNotNull();
        assertThat(response.birthDate()).isNotNull();
    }
}

