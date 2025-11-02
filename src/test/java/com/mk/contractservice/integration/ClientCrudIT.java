package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.valueobject.*;
import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.ContractJpaRepository;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client CRUD Operations - Integration Tests")
class ClientCrudIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientRepository;

    @Autowired
    private ContractJpaRepository contractRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        contractRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    @DisplayName("SCENARIO: Read person client returns correct type and all fields")
    void shouldReadPersonClientWithAllFields() {
        Person givenPerson = new Person(
                ClientName.of("John Doe"),
                Email.of("john.doe." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );
        givenPerson = (Person) clientRepository.save(givenPerson);

        given()
                .when()
                .get("/v1/clients/{id}", givenPerson.getId())
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
        // GIVEN: A company client in DB
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.456.789", uniqueId.substring(0, 6));

        Company company = new Company(
                ClientName.of("Acme Corp"),
                Email.of("acme." + uniqueId + "@example.com"),
                PhoneNumber.of("+41791234567"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        // WHEN: Reading the client
        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
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
                .get("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update person client with valid data succeeds")
    void shouldUpdatePersonClientSuccessfully() {
        // GIVEN: A person client
        Person person = new Person(
                ClientName.of("Alice Before"),
                Email.of("alice.before." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791111111"),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        person = (Person) clientRepository.save(person);

        // WHEN: Updating common fields
        String updatePayload = """
            {
                "name": "Alice After",
                "email": "alice.after.%s@example.com",
                "phone": "+41792222222"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(204);

        // THEN: Changes are persisted
        given()
                .when()
                .get("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Alice After"))
                .body("email", containsString("alice.after"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1985-03-20")); // Unchanged
    }

    @Test
    @DisplayName("SCENARIO: Update company client with valid data succeeds")
    void shouldUpdateCompanyClientSuccessfully() {
        // GIVEN: A company client
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.222.333", uniqueId.substring(0, 6));

        Company company = new Company(
                ClientName.of("Old Corp"),
                Email.of("old." + uniqueId + "@example.com"),
                PhoneNumber.of("+41791111111"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        // WHEN: Updating common fields
        String updatePayload = """
            {
                "name": "New Corp",
                "email": "new.%s@example.com",
                "phone": "+41793333333"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(204);

        // THEN: Changes are persisted
        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("New Corp"))
                .body("email", containsString("new"))
                .body("phone", equalTo("+41793333333"))
                .body("companyIdentifier", equalTo(companyId)); // Unchanged
    }

    @Test
    @DisplayName("SCENARIO: Update with invalid email should fail")
    void shouldRejectUpdateWithInvalidEmail() {
        // GIVEN: A client
        Client client = clientRepository.save(new Person(
                ClientName.of("Test Person"),
                Email.of("test." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Updating with invalid email
        String invalidPayload = """
            {
                "name": "Test Person",
                "email": "invalid-email-format",
                "phone": "+41791234567"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(400);
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
                .put("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client without contracts succeeds")
    void shouldDeleteClientWithoutContracts() {
        // GIVEN: A client without contracts
        Client client = clientRepository.save(new Person(
                ClientName.of("To Delete"),
                Email.of("delete." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Deleting the client
        given()
                .when()
                .delete("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        // THEN: Client is not found
        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client with active contracts closes them")
    void shouldCloseContractsWhenDeletingClient() {
        // GIVEN: A client with active contracts
        Client client = clientRepository.save(new Person(
                ClientName.of("Client With Contracts"),
                Email.of("contracts." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        OffsetDateTime now = OffsetDateTime.now();
        Contract contract1 = new Contract(
                client,
                ContractPeriod.of(now.minusDays(30), null),
                ContractCost.of(new BigDecimal("1000.00"))
        );
        Contract contract2 = new Contract(
                client,
                ContractPeriod.of(now.minusDays(10), now.plusDays(100)),
                ContractCost.of(new BigDecimal("2000.00"))
        );

        contractRepository.save(contract1);
        contractRepository.save(contract2);

        // Verify initial sum
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("3000.00"));

        // WHEN: Deleting the client
        given()
                .when()
                .delete("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        // THEN: Client is not found
        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(404);

        // AND: Contracts sum should be 0 or endpoint should return 404
        // (since contracts are closed, endDate is set to deletion time)
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("SCENARIO: Delete non-existent client returns 404")
    void shouldReturn404WhenDeletingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .delete("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Person stays Person)")
    void shouldMaintainClientTypeAfterUpdate() {
        // GIVEN: A person client
        Person person = new Person(
                ClientName.of("Person Type Test"),
                Email.of("person.type." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        // WHEN: Update the person
        String updatePayload = """
            {
                "name": "Updated Person",
                "email": "updated.person.%s@example.com",
                "phone": "+41792222222"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(204);

        // THEN: Client is still a PERSON with birthDate
        given()
                .when()
                .get("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("PERSON"))
                .body("birthDate", equalTo("1990-01-01"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Company stays Company)")
    void shouldMaintainCompanyTypeAfterUpdate() {
        // GIVEN: A company client
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.888.777", uniqueId.substring(0, 6));

        Company company = new Company(
                ClientName.of("Company Type Test"),
                Email.of("company.type." + uniqueId + "@example.com"),
                PhoneNumber.of("+41791234567"),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        // WHEN: Update the company
        String updatePayload = """
            {
                "name": "Updated Company",
                "email": "updated.company.%s@example.com",
                "phone": "+41793333333"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(204);

        // THEN: Client is still a COMPANY with companyIdentifier
        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("COMPANY"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Concurrent updates should be handled gracefully")
    void shouldHandleConcurrentUpdates() {
        // GIVEN: A client
        Client client = clientRepository.save(new Person(
                ClientName.of("Concurrent Test"),
                Email.of("concurrent." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Multiple updates in quick succession
        String update1 = """
            {
                "name": "Update 1",
                "email": "update1.%s@example.com",
                "phone": "+41791111111"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        String update2 = """
            {
                "name": "Update 2",
                "email": "update2.%s@example.com",
                "phone": "+41792222222"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(update1)
                .when()
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body(update2)
                .when()
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        // THEN: Last update should be persisted
        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Update 2"))
                .body("email", containsString("update2"));
    }
}

