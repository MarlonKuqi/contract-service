package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Client - Domain Entity Tests")
class ClientTest {

    @Nested
    @DisplayName("withCommonFields - Immutable update pattern: Creates new instance with updated fields")
    class WithCommonFieldsValidation {

        @ParameterizedTest
        @MethodSource("personUpdateCombinations")
        @DisplayName("GIVEN valid fields WHEN creating updated person instance THEN new instance has updated fields and original is unchanged")
        void shouldCreateNewPersonInstanceWithUpdatedFields(ClientTestData.CommonClientData updatedData) {
            UUID personId = UUID.randomUUID();
            final Person original = Person.builder()
                    .id(personId)
                    .name(ClientName.of("John Doe"))
                    .email(ClientEmail.of("john.doe@example.com"))
                    .phone(ClientPhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            final Person updated = original.changeCoreFields(
                    ClientName.of(updatedData.name()),
                    ClientEmail.of(updatedData.email()),
                    ClientPhoneNumber.of(updatedData.phone())
            );

            assertThat(updated.getName().getValue()).isEqualTo(updatedData.name());
            assertThat(updated.getEmail().getValue()).isEqualTo(updatedData.email());
            assertThat(updated.getPhone().getValue()).isEqualTo(updatedData.expectedPhone());

            assertThat(updated.getId()).isNotNull();
            assertThat(updated.getId()).isEqualTo(original.getId());
            assertThat(updated.getId()).isEqualTo(personId);

            assertThat(updated.getBirthDate()).isEqualTo(original.getBirthDate());
        }

        static Stream<Object[]> personUpdateCombinations() {
            return ClientTestData.validCommonClientsAsArguments();
        }

        @ParameterizedTest
        @MethodSource("companyUpdateCombinations")
        @DisplayName("GIVEN valid fields WHEN creating updated company instance THEN new instance has updated fields and original is unchanged")
        void shouldCreateNewCompanyInstanceWithUpdatedFields(ClientTestData.CommonClientData updatedData) {
            UUID companyId = UUID.randomUUID();
            String companyIdentifier = "company-123";

            Company originalCompany = Company.builder()
                    .id(companyId)
                    .name(ClientName.of("ACME Corp"))
                    .email(ClientEmail.of("contact@acme.com"))
                    .phone(ClientPhoneNumber.of("+33123456789"))
                    .companyIdentifier(CompanyIdentifier.of(companyIdentifier))
                    .build();

            Company updatedCompany = originalCompany.changeCoreFields(
                    ClientName.of(updatedData.name()),
                    ClientEmail.of(updatedData.email()),
                    ClientPhoneNumber.of(updatedData.phone())
            );

            assertThat(updatedCompany.getName().getValue()).isEqualTo(updatedData.name());
            assertThat(updatedCompany.getEmail().getValue()).isEqualTo(updatedData.email());
            assertThat(updatedCompany.getPhone().getValue()).isEqualTo(updatedData.expectedPhone());

            assertThat(updatedCompany.getId()).isNotNull();
            assertThat(updatedCompany.getId()).isEqualTo(originalCompany.getId());
            assertThat(updatedCompany.getId()).isEqualTo(companyId);

            assertThat(updatedCompany.getCompanyIdentifier()).isEqualTo(originalCompany.getCompanyIdentifier());
            assertThat(updatedCompany.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);
        }

        static Stream<Object[]> companyUpdateCombinations() {
            return ClientTestData.validCommonClientsAsArguments();
        }

        @ParameterizedTest
        @MethodSource("nullFieldCombinationsForPerson")
        @DisplayName("GIVEN null fields WHEN updating person THEN throw InvalidDomainObjectError")
        void shouldRejectNullFieldsOnPersonCoreUpdate(ClientName name, ClientEmail email, ClientPhoneNumber phone) {
            Person person = Person.builder()
                    .id(UUID.randomUUID())
                    .name(ClientName.of("Test User"))
                    .email(ClientEmail.of("test@example.com"))
                    .phone(ClientPhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThatThrownBy(() -> person.changeCoreFields(name, email, phone))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        static Stream<Object[]> nullFieldCombinationsForPerson() {
            return Stream.of(
                    new Object[]{null, ClientEmail.of("test@test.com"), ClientPhoneNumber.of("+33123456789")},
                    new Object[]{ClientName.of("Test"), null, ClientPhoneNumber.of("+33123456789")},
                    new Object[]{ClientName.of("Test"), ClientEmail.of("test@test.com"), null}
            );
        }

        @ParameterizedTest
        @MethodSource("nullFieldCombinationsForCompany")
        @DisplayName("GIVEN null fields WHEN updating company THEN throw InvalidDomainObjectError")
        void shouldRejectNullFieldsOnCompanyCoreUpdate(ClientName name, ClientEmail email, ClientPhoneNumber phone) {
            Company company = Company.builder()
                    .id(UUID.randomUUID())
                    .name(ClientName.of("Test Company"))
                    .email(ClientEmail.of("test@example.com"))
                    .phone(ClientPhoneNumber.of("+33123456789"))
                    .companyIdentifier(CompanyIdentifier.of("valid-identifier"))
                    .build();

            assertThatThrownBy(() -> company.changeCoreFields(name, email, phone))
                    .isInstanceOf(InvalidDomainObjectError.class)
                    .hasMessageContaining("Null found for a non-null model attribute");
        }

        static Stream<Object[]> nullFieldCombinationsForCompany() {
            return Stream.of(
                    new Object[]{null, ClientEmail.of("test@test.com"), ClientPhoneNumber.of("+33123456789")},
                    new Object[]{ClientName.of("Test"), null, ClientPhoneNumber.of("+33123456789")},
                    new Object[]{ClientName.of("Test"), ClientEmail.of("test@test.com"), null}
            );
        }
    }

}

