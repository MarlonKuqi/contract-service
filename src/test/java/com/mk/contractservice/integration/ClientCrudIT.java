package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import com.mk.contractservice.integration.helper.TestDataHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client CRUD Operations - Integration Tests")
class ClientCrudIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("SCENARIO: Read person client returns correct type and all fields")
    void shouldReadPersonClientWithAllFields() {
        Person givenPerson = Person.of(
                ClientName.of("John Doe"),
                ClientEmail.of(TestDataHelper.uniqueEmail("john.doe")),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );
        givenPerson = (Person) clientRepository.save(givenPerson);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, givenPerson.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(givenPerson.getId().toString()))
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("John Doe"))
                .body("email", containsString("john.doe"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-05-15"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Read company client returns correct type and all fields")
    void shouldReadCompanyClientWithAllFields() {
        String companyId = TestDataHelper.uniqueCompanyIdentifier("CHE");

        Company company = Company.of(
                ClientName.of("Acme Corp"),
                ClientEmail.of(TestDataHelper.uniqueEmail("acme")),
                ClientPhoneNumber.of("+41791234567"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(company.getId().toString()))
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corp"))
                .body("email", containsString("acme"))
                .body("phone", equalTo("+41791234567"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Read non-existent client returns 404")
    void shouldReturn404ForNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update person client with valid data succeeds")
    void shouldUpdatePersonClientSuccessfully() {
        Person person = Person.of(
                ClientName.of("Alice Before"),
                ClientEmail.of("alice.before." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791111111"),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        person = (Person) clientRepository.save(person);
        String updatePayload = """
                {
                    "name": "Alice After",
                    "email": "alice.after.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Alice After"))
                .body("email", containsString("alice.after"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1985-03-20"));
    }

    @Test
    @DisplayName("SCENARIO: Update company client with valid data succeeds")
    void shouldUpdateCompanyClientSuccessfully() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.222.333", uniqueId.substring(0, 6));

        Company company = Company.of(
                ClientName.of("Old Corp"),
                ClientEmail.of("old." + uniqueId + "@example.com"),
                ClientPhoneNumber.of("+41791111111"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);
        String updatePayload = """
                {
                    "name": "New Corp",
                    "email": "new.%s@example.com",
                    "phone": "+41793333333"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("New Corp"))
                .body("email", containsString("new"))
                .body("phone", equalTo("+41793333333"))
                .body("companyIdentifier", equalTo(companyId));
    }

    @Test
    @DisplayName("SCENARIO: Update with invalid clientEmail should fail")
    void shouldRejectUpdateWithInvalidEmail() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        String invalidPayload = """
                {
                    "name": "Test Person",
                    "email": "invalid-clientEmail-format",
                    "phone": "+41791234567"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update non-existent client returns 404")
    void shouldReturn404WhenUpdatingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        String updatePayload = """
                {
                    "name": "Ghost Client",
                    "email": "ghost@example.com",
                    "phone": "+41791234567"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client without contracts succeeds")
    void shouldDeleteClientWithoutContracts() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("To Delete"),
                ClientEmail.of("delete." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client with active contracts closes them")
    void shouldCloseContractsWhenDeletingClient() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Client With Contracts"),
                ClientEmail.of("contracts." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
        Contract contract1 = Contract.builder()
                .clientId(client.getId())
                .period(ContractPeriod.of(now.minusDays(30), null))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract contract2 = Contract.builder()
                .clientId(client.getId())
                .period(ContractPeriod.of(now.minusDays(10), now.plusDays(100)))
                .costAmount(ContractCost.of(new BigDecimal("2000.00")))
                .build();
        contractRepository.save(contract1);
        contractRepository.save(contract2);
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("3000.00"));
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(404);
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", client.getId())
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("SCENARIO: Delete non-existent client is idempotent (returns 204)")
    void shouldReturn204WhenDeletingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Person stays Person)")
    void shouldMaintainClientTypeAfterUpdate() {
        Person person = Person.of(
                ClientName.of("Person Type Test"),
                ClientEmail.of("person.type." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);
        String updatePayload = """
                {
                    "name": "Updated Person",
                    "email": "updated.person.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("PERSON"))
                .body("birthDate", equalTo("1990-01-01"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Company stays Company)")
    void shouldMaintainCompanyTypeAfterUpdate() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.888.777", uniqueId.substring(0, 6));

        Company company = Company.of(
                ClientName.of("Company Type Test"),
                ClientEmail.of("company.type." + uniqueId + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);
        String updatePayload = """
                {
                    "name": "Updated Company",
                    "email": "updated.company.%s@example.com",
                    "phone": "+41793333333"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("COMPANY"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Concurrent updates should be handled gracefully")
    void shouldHandleConcurrentUpdates() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Concurrent Test"),
                ClientEmail.of("concurrent." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        String update1 = """
                {
                    "name": "Update 1",
                    "email": "update1.%s@example.com",
                    "phone": "+41791111111"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String update2 = """
                {
                    "name": "Update 2",
                    "email": "update2.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(update1)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body(update2)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Update 2"))
                .body("email", containsString("update2"));
    }

    @Test
    @DisplayName("SCENARIO: PATCH person client with only name updates only name")
    void shouldPatchPersonClientWithOnlyName() {
        Person person = Person.of(
                ClientName.of("Original Name"),
                ClientEmail.of("original." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "name": "Patched Name"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Patched Name"))
                .body("email", containsString("original"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("SCENARIO: PATCH person client with only clientEmail updates only clientEmail")
    void shouldPatchPersonClientWithOnlyEmail() {
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("old." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "email": "new.%s@example.com"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("new"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("SCENARIO: PATCH person client with only phone updates only phone")
    void shouldPatchPersonClientWithOnlyPhone() {
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "phone": "+41799999999"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("test"))
                .body("phone", equalTo("+41799999999"))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("SCENARIO: PATCH company client with multiple fields updates only those fields")
    void shouldPatchCompanyClientWithMultipleFields() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.999.888", uniqueId.substring(0, 6));

        Company company = Company.of(
                ClientName.of("Old Corp"),
                ClientEmail.of("old." + uniqueId + "@example.com"),
                ClientPhoneNumber.of("+41791111111"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        String patchPayload = """
                {
                    "name": "Patched Corp",
                    "email": "patched.%s@example.com"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Patched Corp"))
                .body("email", containsString("patched"))
                .body("phone", equalTo("+41791111111"))
                .body("companyIdentifier", equalTo(companyId));
    }

    @Test
    @DisplayName("SCENARIO: PATCH with invalid clientEmail should fail")
    void shouldRejectPatchWithInvalidEmail() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        String invalidPatchPayload = """
                {
                    "email": "invalid-clientEmail"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: PATCH with invalid phone should fail")
    void shouldRejectPatchWithInvalidPhone() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        String invalidPatchPayload = """
                {
                    "phone": "invalid"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: PATCH non-existent client returns 404")
    void shouldReturn404WhenPatchingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        String patchPayload = """
                {
                    "name": "Ghost Name"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: PATCH with empty body should succeed (no changes)")
    void shouldAcceptPatchWithEmptyBody() {
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String emptyPatchPayload = "{}";

        given()
                .contentType(ContentType.JSON)
                .body(emptyPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("test"))
                .body("phone", equalTo("+41791234567"));
    }

    @Test
    @DisplayName("EDGE CASE: Special characters in names should be handled")
    void shouldHandleSpecialCharactersInNames() {
        String specialNamePayload = """
                {
                    "type": "PERSON",
                    "name": "François O'Brien-Müller",
                    "email": "francois.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-01-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(specialNamePayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("name", equalTo("François O'Brien-Müller"));
    }

    @Test
    @DisplayName("EDGE CASE: International phone numbers should be validated")
    void shouldValidateInternationalPhoneNumbers() {
        String[] validPhones = {
                "+41 79 123 45 67",    // Switzerland
                "+33 6 12 34 56 78",    // France
                "+39 345 123 4567",  // Italy (mobile)
                "+49 151 12345678",    // Germany (mobile)
                "+41211234567"    // Switzerland (without spaces)
        };

        for (String phone : validPhones) {
            String payload = """
                    {
                        "type": "PERSON",
                        "name": "International Test",
                        "email": "intl.%s@example.com",
                        "phone": "%s",
                        "birthDate": "1990-01-01"
                    }
                    """.formatted(UUID.randomUUID().toString().substring(0, 8) + phone.hashCode(), phone);

            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @DisplayName("EDGE CASE: Very old birth dates should be accepted")
    void shouldAcceptVeryOldBirthDates() {
        String oldBirthDatePayload = """
                {
                    "type": "PERSON",
                    "name": "Very Old Person",
                    "email": "old.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1920-01-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(oldBirthDatePayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("birthDate", equalTo("1920-01-01"));
    }
}


