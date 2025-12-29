package com.mk.contractservice.domain.client.factory;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.shared.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CompanyFactory")
class CompanyFactoryTest {

    @Nested
    @DisplayName("Création d'entreprise")
    class CreateCompany {

        @Test
        @DisplayName("GIVEN paramètres valides WHEN create THEN retourne une entreprise avec les bonnes valeurs")
        void shouldCreateCompanyWithCorrectValues() {
            // Given
            String name = "TechCorp SARL";
            String email = "contact@techcorp.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "123456789";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company).isNotNull();
            assertThat(company.getName().getValue()).isEqualTo(name);
            assertThat(company.getEmail().getValue()).isEqualTo(email.toLowerCase());
            assertThat(company.getPhone().getValue()).isEqualTo(phoneNumber);
            assertThat(company.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);
        }

        @Test
        @DisplayName("GIVEN email avec majuscules WHEN create THEN normalise en minuscules")
        void shouldNormalizeEmailToLowercase() {
            // Given
            String name = "TechCorp SARL";
            String email = "Contact@TechCorp.COM";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "123456789";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getEmail().getValue()).isEqualTo("contact@techcorp.com");
        }
    }

    @Nested
    @DisplayName("Validation des paramètres")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN name null WHEN create THEN lève une exception")
        void shouldRejectNullName() {
            // Given
            String email = "contact@techcorp.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "123456789";

            // When & Then
            assertThatThrownBy(() -> CompanyFactory.create(null, email, phoneNumber, companyIdentifier))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN email null WHEN create THEN lève une exception")
        void shouldRejectNullEmail() {
            // Given
            String name = "TechCorp SARL";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "123456789";

            // When & Then
            assertThatThrownBy(() -> CompanyFactory.create(name, null, phoneNumber, companyIdentifier))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN phoneNumber null WHEN create THEN lève une exception")
        void shouldRejectNullPhoneNumber() {
            // Given
            String name = "TechCorp SARL";
            String email = "contact@techcorp.com";
            String companyIdentifier = "123456789";

            // When & Then
            assertThatThrownBy(() -> CompanyFactory.create(name, email, null, companyIdentifier))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN companyIdentifier null WHEN create THEN lève une exception")
        void shouldRejectNullCompanyIdentifier() {
            // Given
            String name = "TechCorp SARL";
            String email = "contact@techcorp.com";
            String phoneNumber = "+33123456789";

            // When & Then
            assertThatThrownBy(() -> CompanyFactory.create(name, email, phoneNumber, null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("GIVEN nom d'entreprise long WHEN create THEN crée l'entreprise")
        void shouldCreateCompanyWithLongName() {
            // Given
            String name = "Société Internationale de Technologie et d'Innovation Numérique";
            String email = "contact@sitin.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "123456789";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getName().getValue()).isEqualTo(name);
        }

        @Test
        @DisplayName("GIVEN identifiant avec caractères spéciaux WHEN create THEN crée l'entreprise")
        void shouldCreateCompanyWithSpecialCharactersInIdentifier() {
            // Given
            String name = "TechCorp SARL";
            String email = "contact@techcorp.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "FR-123456789";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);
        }

        @Test
        @DisplayName("GIVEN numéro international WHEN create THEN crée l'entreprise")
        void shouldCreateCompanyWithInternationalPhone() {
            // Given
            String name = "TechCorp UK";
            String email = "contact@techcorp.co.uk";
            String phoneNumber = "+441234567890";
            String companyIdentifier = "UK123456789";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getPhone().getValue()).isEqualTo(phoneNumber);
        }

        @Test
        @DisplayName("GIVEN nom avec symboles WHEN create THEN crée l'entreprise")
        void shouldCreateCompanyWithSymbolsInName() {
            // Given
            String name = "Tech & Co.";
            String email = "contact@techco.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "987654321";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getName().getValue()).isEqualTo(name);
        }

        @Test
        @DisplayName("GIVEN identifiant numérique WHEN create THEN crée l'entreprise")
        void shouldCreateCompanyWithNumericIdentifier() {
            // Given
            String name = "TechCorp SARL";
            String email = "contact@techcorp.com";
            String phoneNumber = "+33123456789";
            String companyIdentifier = "999888777666555";

            // When
            Company company = CompanyFactory.create(name, email, phoneNumber, companyIdentifier);

            // Then
            assertThat(company.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);
        }
    }
}

