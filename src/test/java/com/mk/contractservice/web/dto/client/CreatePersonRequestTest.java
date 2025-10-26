package com.mk.contractservice.web.dto.client;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CreatePersonRequest DTO Validation")
class CreatePersonRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should accept valid CreatePersonRequest")
    void shouldAcceptValidRequest() {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject request with blank name")
    void shouldRejectBlankName() {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "   ",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactly("name");
    }

    @Test
    @DisplayName("Should reject request with name exceeding 200 characters")
    void shouldRejectNameTooLong() {
        // Given
        String longName = "a".repeat(201);
        CreatePersonRequest request = new CreatePersonRequest(
                longName,
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactly("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-email",
            "@example.com",
            "user@",
            "user @example.com"
    })
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidEmail(String invalidEmail) {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                invalidEmail,
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("email");
    }

    @Test
    @DisplayName("Should reject email exceeding 254 characters")
    void shouldRejectEmailTooLong() {
        // Given
        String longEmail = "a".repeat(245) + "@test.com";
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                longEmail,
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactly("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",           // Too short
            "123456",        // No country code
            "abc-def-ghi"    // Invalid characters
    })
    @DisplayName("Should reject invalid phone patterns")
    void shouldRejectInvalidPhone(String invalidPhone) {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                invalidPhone,
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+33123456789",
            "+1234567890",
            "0123456789",
            "+33 1 23 45 67 89",
            "+33-1-23-45-67-89",
            "+33.1.23.45.67.89",
            "+33 (1) 23-45-67-89"
    })
    @DisplayName("Should accept valid phone patterns")
    void shouldAcceptValidPhone(String validPhone) {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                validPhone,
                LocalDate.of(1990, 1, 1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject null birthDate")
    void shouldRejectNullBirthDate() {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                null
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactly("birthDate");
    }

    @Test
    @DisplayName("Should accept future birthDate (no @Past validation)")
    void shouldAcceptFutureBirthDate() {
        // Given - Future date (unusual but not validated in DTO)
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.now().plusYears(1)
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        // Note: Business validation for realistic birthDate would be in service layer
    }

    @Test
    @DisplayName("Should reject request with multiple invalid fields")
    void shouldRejectMultipleInvalidFields() {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "",                    // Invalid: blank
                "invalid-email",       // Invalid: format
                "abc",                 // Invalid: pattern
                null                   // Invalid: null
        );

        // When
        Set<ConstraintViolation<CreatePersonRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(4);
    }
}

