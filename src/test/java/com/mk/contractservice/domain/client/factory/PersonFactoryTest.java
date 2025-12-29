package com.mk.contractservice.domain.client.factory;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.shared.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PersonFactory")
class PersonFactoryTest {

    @Nested
    @DisplayName("Création de personne")
    class CreatePerson {

        @Test
        @DisplayName("GIVEN paramètres valides WHEN create THEN retourne une personne avec les bonnes valeurs")
        void shouldCreatePersonWithCorrectValues() {
            // Given
            String name = "Jean Dupont";
            String email = "jean.dupont@example.com";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person).isNotNull();
            assertThat(person.getName().getValue()).isEqualTo(name);
            assertThat(person.getEmail().getValue()).isEqualTo(email.toLowerCase());
            assertThat(person.getPhone().getValue()).isEqualTo(phoneNumber);
            assertThat(person.getBirthDate().getValue()).isEqualTo(birthDate);
        }

        @Test
        @DisplayName("GIVEN email avec majuscules WHEN create THEN normalise en minuscules")
        void shouldNormalizeEmailToLowercase() {
            // Given
            String name = "Jean Dupont";
            String email = "Jean.DUPONT@Example.COM";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person.getEmail().getValue()).isEqualTo("jean.dupont@example.com");
        }
    }

    @Nested
    @DisplayName("Validation des paramètres")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN name null WHEN create THEN lève une exception")
        void shouldRejectNullName() {
            // Given
            String email = "jean.dupont@example.com";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When & Then
            assertThatThrownBy(() -> PersonFactory.create(null, email, phoneNumber, birthDate))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN email null WHEN create THEN lève une exception")
        void shouldRejectNullEmail() {
            // Given
            String name = "Jean Dupont";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When & Then
            assertThatThrownBy(() -> PersonFactory.create(name, null, phoneNumber, birthDate))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN phoneNumber null WHEN create THEN lève une exception")
        void shouldRejectNullPhoneNumber() {
            // Given
            String name = "Jean Dupont";
            String email = "jean.dupont@example.com";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When & Then
            assertThatThrownBy(() -> PersonFactory.create(name, email, null, birthDate))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN birthDate null WHEN create THEN lève une exception")
        void shouldRejectNullBirthDate() {
            // Given
            String name = "Jean Dupont";
            String email = "jean.dupont@example.com";
            String phoneNumber = "+33612345678";

            // When & Then
            assertThatThrownBy(() -> PersonFactory.create(name, email, phoneNumber, null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("GIVEN nom avec caractères spéciaux WHEN create THEN crée la personne")
        void shouldCreatePersonWithSpecialCharactersInName() {
            // Given
            String name = "Jean-Pierre O'Connor";
            String email = "jp@example.com";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person.getName().getValue()).isEqualTo(name);
        }

        @Test
        @DisplayName("GIVEN personne très jeune WHEN create THEN crée la personne")
        void shouldCreateYoungPerson() {
            // Given
            String name = "Jeune Personne";
            String email = "jeune@example.com";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.now().minusYears(18);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person.getBirthDate().getValue()).isEqualTo(birthDate);
        }

        @Test
        @DisplayName("GIVEN personne âgée WHEN create THEN crée la personne")
        void shouldCreateElderlyPerson() {
            // Given
            String name = "Personne Âgée";
            String email = "senior@example.com";
            String phoneNumber = "+33612345678";
            LocalDate birthDate = LocalDate.of(1930, 1, 1);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person.getBirthDate().getValue()).isEqualTo(birthDate);
        }

        @Test
        @DisplayName("GIVEN numéro international WHEN create THEN crée la personne")
        void shouldCreatePersonWithInternationalPhone() {
            // Given
            String name = "Jean Dupont";
            String email = "jean@example.com";
            String phoneNumber = "+441234567890";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            // When
            Person person = PersonFactory.create(name, email, phoneNumber, birthDate);

            // Then
            assertThat(person.getPhone().getValue()).isEqualTo(phoneNumber);
        }
    }
}

