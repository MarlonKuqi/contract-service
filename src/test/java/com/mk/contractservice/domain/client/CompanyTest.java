package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.exception.InvalidClientEmailException;
import com.mk.contractservice.domain.client.exception.InvalidClientNameException;
import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.client.exception.InvalidCompanyIdentifierException;
import com.mk.contractservice.domain.client.factory.CompanyFactory;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Company - Domain Tests")
class CompanyTest {

    static Stream<String> invalidCompanyIdentifiers() {
        return Stream.of(
                null,
                "",
                "   ",
                "a".repeat(CompanyIdentifier.MAX_LENGTH + 1)
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Nested
        @DisplayName("From Command")
        class FromCommand {
            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidEmails")
            @DisplayName("GIVEN invalid email WHEN creating Company THEN throw exception")
            void shouldRejectInvalidEmail(String email) {
                assertThatThrownBy(() -> CompanyFactory.createFromCommand(
                        "ACME Corp",
                        email,
                        "+33123456789",
                        "acme-123"
                )).isInstanceOf(InvalidClientEmailException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidNames")
            @DisplayName("GIVEN invalid name WHEN creating Company THEN throw exception")
            void shouldRejectInvalidName(String name) {
                assertThatThrownBy(() -> CompanyFactory.createFromCommand(
                        name,
                        "contact@acme.com",
                        "+33123456789",
                        "acme-123"
                )).isInstanceOf(InvalidClientNameException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidPhones")
            @DisplayName("GIVEN invalid phone WHEN creating Company THEN throw exception")
            void shouldRejectInvalidPhone(String phone) {
                assertThatThrownBy(() -> CompanyFactory.createFromCommand(
                        "ACME Corp",
                        "contact@acme.com",
                        phone,
                        "acme-123"
                )).isInstanceOf(InvalidClientPhoneNumberException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.CompanyTest#invalidCompanyIdentifiers")
            @DisplayName("GIVEN invalid company identifier WHEN creating Company THEN throw exception")
            void shouldRejectInvalidCompanyIdentifier(String identifier) {
                assertThatThrownBy(() -> CompanyFactory.createFromCommand(
                        "ACME Corp",
                        "contact@acme.com",
                        "+33123456789",
                        identifier
                )).isInstanceOf(InvalidCompanyIdentifierException.class);
            }


            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#validCompanies")
            @DisplayName("GIVEN various valid combinations of fields WHEN creating Company THEN company is created with all correct values")
            void shouldCreateCompanyWithValidCombinations(ClientTestData.ValidCompanyData data) {
                final Company company = CompanyFactory.createFromCommand(
                        data.name(),
                        data.email(),
                        data.phone(),
                        data.companyIdentifier()
                );

                assertThat(company.getName().getValue()).isEqualTo(data.name());
                assertThat(company.getEmail().getValue()).isEqualTo(data.email());
                assertThat(company.getPhone().getValue()).isEqualTo(data.expectedPhone());
                assertThat(company.getCompanyIdentifier().getValue()).isEqualTo(data.companyIdentifier());
            }

            @Test
            @DisplayName("GIVEN company identifier at max length WHEN creating Company THEN company is created")
            void shouldCreateCompanyWithMaxLengthIdentifier() {
                final String maxLengthIdentifier = "a".repeat(CompanyIdentifier.MAX_LENGTH);

                final Company company = CompanyFactory.createFromCommand(
                        "Max Length Corp",
                        "max@length.com",
                        "+33123456789",
                        maxLengthIdentifier
                );

                assertThat(company.getCompanyIdentifier().getValue()).hasSize(CompanyIdentifier.MAX_LENGTH);
            }
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Test
        @DisplayName("GIVEN company WHEN updating common fields THEN company identifier remains unchanged")
        void shouldKeepCompanyIdentifierImmutableWhenUpdating() {
            final CompanyIdentifier originalIdentifier = CompanyIdentifier.of("acme-123");
            final Company company = Company.of(
                    ClientName.of("ACME Corp"),
                    ClientEmail.of("contact@acme.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    originalIdentifier
            );

            final Company updated = company.changeCoreFields(
                    ClientName.of("ACME Corporation"),
                    ClientEmail.of("info@acme.com"),
                    ClientPhoneNumber.of("+33987654321")
            );

            assertThat(updated.getName().getValue()).isEqualTo("ACME Corporation");
            assertThat(updated.getEmail().getValue()).isEqualTo("info@acme.com");
            assertThat(updated.getPhone().getValue()).isEqualTo("+33987654321");
            assertThat(updated.getCompanyIdentifier()).isEqualTo(originalIdentifier);

            assertThat(company.getName().getValue()).isEqualTo("ACME Corp");
            assertThat(company.getCompanyIdentifier()).isEqualTo(originalIdentifier);
        }

        @Test
        @DisplayName("GIVEN company WHEN updating with same values THEN returns new instance with same data")
        void shouldReturnNewInstanceWithSameData() {
            final Company company = CompanyFactory.createFromCommand(
                    "ACME Corp",
                    "contact@acme.com",
                    "+33123456789",
                    "acme-123"
            );

            final Company updated = company.changeCoreFields(
                    ClientName.of("ACME Corp"),
                    ClientEmail.of("contact@acme.com"),
                    ClientPhoneNumber.of("+33123456789")
            );

            assertThat(updated).isNotSameAs(company);
            assertThat(updated.getName().getValue()).isEqualTo(company.getName().getValue());
            assertThat(updated.getEmail().getValue()).isEqualTo(company.getEmail().getValue());
            assertThat(updated.getPhone().getValue()).isEqualTo(company.getPhone().getValue());
        }

        @Test
        @DisplayName("GIVEN company WHEN updating only name THEN only name changes")
        void shouldUpdateOnlyName() {
            final Company company = CompanyFactory.createFromCommand(
                    "ACME Corp",
                    "contact@acme.com",
                    "+33123456789",
                    "acme-123"
            );

            final Company updated = company.changeCoreFields(
                    ClientName.of("New ACME Name"),
                    company.getEmail(),
                    company.getPhone()
            );

            assertThat(updated.getName().getValue()).isEqualTo("New ACME Name");
            assertThat(updated.getEmail()).isEqualTo(company.getEmail());
            assertThat(updated.getPhone()).isEqualTo(company.getPhone());
            assertThat(updated.getCompanyIdentifier()).isEqualTo(company.getCompanyIdentifier());
        }

        @Test
        @DisplayName("GIVEN company WHEN updating only email THEN only email changes")
        void shouldUpdateOnlyEmail() {
            final Company company = CompanyFactory.createFromCommand(
                    "ACME Corp",
                    "contact@acme.com",
                    "+33123456789",
                    "acme-123"
            );

            final Company updated = company.changeCoreFields(
                    company.getName(),
                    ClientEmail.of("newemail@acme.com"),
                    company.getPhone()
            );

            assertThat(updated.getName()).isEqualTo(company.getName());
            assertThat(updated.getEmail().getValue()).isEqualTo("newemail@acme.com");
            assertThat(updated.getPhone()).isEqualTo(company.getPhone());
        }

        @Test
        @DisplayName("GIVEN company WHEN updating only phone THEN only phone changes")
        void shouldUpdateOnlyPhone() {
            final Company company = CompanyFactory.createFromCommand(
                    "ACME Corp",
                    "contact@acme.com",
                    "+33123456789",
                    "acme-123"
            );

            final Company updated = company.changeCoreFields(
                    company.getName(),
                    company.getEmail(),
                    ClientPhoneNumber.of("+33999999999")
            );

            assertThat(updated.getName()).isEqualTo(company.getName());
            assertThat(updated.getEmail()).isEqualTo(company.getEmail());
            assertThat(updated.getPhone().getValue()).isEqualTo("+33999999999");
        }
    }
}

