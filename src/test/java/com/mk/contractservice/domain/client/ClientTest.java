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
            final Person original = PersonFactory.buildFromDatabase(
                    personId,
                    "John Doe",
                    "john.doe@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 1, 1)
            );

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

            Company originalCompany = CompanyFactory.buildFromDatabase(
                    companyId,
                    "ACME Corp",
                    "contact@acme.com",
                    "+33123456789",
                    companyIdentifier
            );

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
            Person person = PersonFactory.createFromCommand(
                    "Test User",
                    "test@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 1, 1),
                    LocalDate.now()
            );

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
            Company company = CompanyFactory.createFromCommand(
                    "Test Company",
                    "test@example.com",
                    "+33123456789",
                    "valid-identifier"
            );

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

    @Nested
    @DisplayName("hasName / hasEmail / hasPhone - Compares normalized values")
    class HasCoreFields {

        static Stream<Client> clients() {
            return Stream.of(
                    PersonFactory.createFromCommand("John Doe", "john.doe@example.com", "+33123456789", LocalDate.of(1990, 1, 1), LocalDate.now()),
                    CompanyFactory.createFromCommand("John Doe", "john.doe@example.com", "+33123456789", "acme-123")
            );
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN same values written differently WHEN comparing THEN client has them")
        void shouldHaveSameValuesWrittenDifferently(Client client) {
            assertThat(client.hasName(ClientName.of("  John Doe  "))).isTrue();
            assertThat(client.hasEmail(ClientEmail.of("JOHN.DOE@Example.com"))).isTrue();
            assertThat(client.hasPhone(ClientPhoneNumber.of("+33 1 23 45 67 89"))).isTrue();
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN different values WHEN comparing THEN client does not have them")
        void shouldNotHaveDifferentValues(Client client) {
            assertThat(client.hasName(ClientName.of("Jane Doe"))).isFalse();
            assertThat(client.hasEmail(ClientEmail.of("jane.doe@example.com"))).isFalse();
            assertThat(client.hasPhone(ClientPhoneNumber.of("+33987654321"))).isFalse();
        }
    }

    @Nested
    @DisplayName("change* - Identical values are a no-op")
    class ChangeNoOp {

        static Stream<Client> clients() {
            return Stream.of(
                    PersonFactory.createFromCommand("John Doe", "john.doe@example.com", "+33123456789", LocalDate.of(1990, 1, 1), LocalDate.now()),
                    CompanyFactory.createFromCommand("John Doe", "john.doe@example.com", "+33123456789", "acme-123")
            );
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN same name written differently WHEN changeName THEN returns the same instance")
        void shouldReturnSameInstanceWhenNameIsIdentical(Client client) {
            assertThat(client.changeName(ClientName.of("  John Doe  "))).isSameAs(client);
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN same email written differently WHEN changeEmail THEN returns the same instance")
        void shouldReturnSameInstanceWhenEmailIsIdentical(Client client) {
            assertThat(client.changeEmail(ClientEmail.of("JOHN.DOE@Example.com"))).isSameAs(client);
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN same phone written differently WHEN changePhone THEN returns the same instance")
        void shouldReturnSameInstanceWhenPhoneIsIdentical(Client client) {
            assertThat(client.changePhone(ClientPhoneNumber.of("+33 1 23 45 67 89"))).isSameAs(client);
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN same core fields written differently WHEN changeCoreFields THEN returns the same instance")
        void shouldReturnSameInstanceWhenCoreFieldsAreIdentical(Client client) {
            final Client result = client.changeCoreFields(
                    ClientName.of("  John Doe  "),
                    ClientEmail.of("JOHN.DOE@Example.com"),
                    ClientPhoneNumber.of("+33 1 23 45 67 89")
            );

            assertThat(result).isSameAs(client);
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN a different name WHEN changeName THEN returns a new instance with the new name")
        void shouldReturnNewInstanceWhenNameDiffers(Client client) {
            final Client result = client.changeName(ClientName.of("Jane Doe"));

            assertThat(result).isNotSameAs(client);
            assertThat(result.getName().getValue()).isEqualTo("Jane Doe");
            assertThat(client.getName().getValue()).isEqualTo("John Doe");
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN a different email WHEN changeEmail THEN returns a new instance with the new email")
        void shouldReturnNewInstanceWhenEmailDiffers(Client client) {
            final Client result = client.changeEmail(ClientEmail.of("jane.doe@example.com"));

            assertThat(result).isNotSameAs(client);
            assertThat(result.getEmail().getValue()).isEqualTo("jane.doe@example.com");
            assertThat(client.getEmail().getValue()).isEqualTo("john.doe@example.com");
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN a different phone WHEN changePhone THEN returns a new instance with the new phone")
        void shouldReturnNewInstanceWhenPhoneDiffers(Client client) {
            final Client result = client.changePhone(ClientPhoneNumber.of("+33987654321"));

            assertThat(result).isNotSameAs(client);
            assertThat(result.getPhone().getValue()).isEqualTo("+33987654321");
            assertThat(client.getPhone().getValue()).isEqualTo("+33123456789");
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN only one core field differs WHEN changeCoreFields THEN returns a new instance")
        void shouldReturnNewInstanceWhenOneCoreFieldDiffers(Client client) {
            final Client result = client.changeCoreFields(
                    client.getName(),
                    client.getEmail(),
                    ClientPhoneNumber.of("+33987654321")
            );

            assertThat(result).isNotSameAs(client);
            assertThat(result.getPhone().getValue()).isEqualTo("+33987654321");
        }

        @ParameterizedTest
        @MethodSource("clients")
        @DisplayName("GIVEN null values WHEN changeName, changeEmail or changePhone THEN throw InvalidDomainObjectError")
        void shouldRejectNullOnSingleFieldChange(Client client) {
            assertThatThrownBy(() -> client.changeName(null)).isInstanceOf(InvalidDomainObjectError.class);
            assertThatThrownBy(() -> client.changeEmail(null)).isInstanceOf(InvalidDomainObjectError.class);
            assertThatThrownBy(() -> client.changePhone(null)).isInstanceOf(InvalidDomainObjectError.class);
        }
    }

}

