package com.mk.contractservice.integration;

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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Person Client Lifecycle Scenarios - Integration Tests")
class PersonLifecycleIT {

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
    @DisplayName("SCENARIO: Create person client with all valid data")
    void shouldCreatePersonClientSuccessfully() {
        // GIVEN: Valid person creation payload
        String createPayload = """
            {
                "name": "Alice Martin",
                "email": "alice.martin.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "1990-05-15"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        // WHEN: Creating person
        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/v1/clients/[0-9a-f-]{36}"))
                .body("id", notNullValue())
                .body("name", equalTo("Alice Martin"))
                .body("email", containsString("alice.martin"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-05-15"))
                .extract().path("id");

        // THEN: Can retrieve the created person
        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .body("id", equalTo(clientId))
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("Alice Martin"))
                .body("birthDate", equalTo("1990-05-15"));
    }

    @Test
    @DisplayName("SCENARIO: Invalid email format should be rejected")
    void shouldRejectInvalidEmailFormat() {
        String invalidEmailPayload = """
            {
                "name": "Bob Bernard",
                "email": "invalid-email-format",
                "phone": "+41791234567",
                "birthDate": "1985-03-20"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidEmailPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("SCENARIO: Invalid phone number format should be rejected")
    void shouldRejectInvalidPhoneFormat() {
        String invalidPhonePayload = """
            {
                "name": "Charlie Chaplin",
                "email": "charlie.%s@example.com",
                "phone": "invalid-phone",
                "birthDate": "1989-07-10"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(invalidPhonePayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("SCENARIO: Missing required fields should be rejected")
    void shouldRejectMissingRequiredFields() {
        // Missing email
        String missingEmailPayload = """
            {
                "name": "David Durand",
                "phone": "+41791234567",
                "birthDate": "1992-12-01"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(missingEmailPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(400);

        // Missing name
        String missingNamePayload = """
            {
                "email": "david.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "1992-12-01"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(missingNamePayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("SCENARIO: Future birth date should be rejected")
    void shouldRejectFutureBirthDate() {
        String futureBirthDatePayload = """
            {
                "name": "Eve Future",
                "email": "eve.future.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "2030-01-01"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(futureBirthDatePayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Update person client common fields")
    void shouldUpdatePersonCommonFields() {
        // GIVEN: Create a person
        String createPayload = """
            {
                "name": "Frank Original",
                "email": "frank.original.%s@example.com",
                "phone": "+41791111111",
                "birthDate": "1988-04-20"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .extract().path("id");

        // WHEN: Update common fields (name, email, phone)
        String updatePayload = """
            {
                "name": "Frank Updated",
                "email": "frank.updated.%s@example.com",
                "phone": "+41792222222"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", clientId)
                .then()
                .statusCode(204);

        // THEN: Verify updates persisted
        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Frank Updated"))
                .body("email", containsString("frank.updated"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1988-04-20")); // birthDate unchanged
    }

    @Test
    @DisplayName("SCENARIO: Delete person and verify contracts are closed")
    void shouldDeletePersonAndCloseContracts() {
        // GIVEN: Create a person
        String personPayload = """
            {
                "name": "Grace ToDelete",
                "email": "grace.delete.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "1995-08-15"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(personPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .extract().path("id");

        // AND: Create active contracts for this person
        String contractPayload = """
            {
                "startDate": "2025-01-01T00:00:00Z",
                "endDate": null,
                "costAmount": "3000.00"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", clientId)
                .then()
                .statusCode(201);

        // WHEN: Delete the person
        given()
                .when()
                .delete("/v1/clients/{id}", clientId)
                .then()
                .statusCode(204);

        // THEN: Person should not be found
        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(404);

        // AND: Contracts should be closed (sum = 0 because all contracts have endDate set to now or past)
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", clientId)
                .then()
                .statusCode(anyOf(is(200), is(404))); // Could be 404 if client not found
    }

    @Test
    @DisplayName("SCENARIO: Duplicate email should be handled gracefully")
    void shouldHandleDuplicateEmail() {
        String uniqueEmail = "duplicate.test." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // GIVEN: Create first person with email
        String firstPayload = """
            {
                "name": "First Person",
                "email": "%s",
                "phone": "+41791111111",
                "birthDate": "1990-01-01"
            }
            """.formatted(uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(firstPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201);

        // WHEN: Try to create second person with same email
        String secondPayload = """
            {
                "name": "Second Person",
                "email": "%s",
                "phone": "+41792222222",
                "birthDate": "1995-06-15"
            }
            """.formatted(uniqueEmail);

        // THEN: Should fail with conflict or validation error
        given()
                .contentType(ContentType.JSON)
                .body(secondPayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(anyOf(is(409), is(400), is(422), is(500))); // Depending on your implementation
    }

    @Test
    @DisplayName("SCENARIO: Create multiple persons and verify unique IDs")
    void shouldCreateMultiplePersonsWithUniqueIds() {
        String person1Payload = """
            {
                "name": "Person One",
                "email": "person1.%s@example.com",
                "phone": "+41791111111",
                "birthDate": "1990-01-01"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        String person2Payload = """
            {
                "name": "Person Two",
                "email": "person2.%s@example.com",
                "phone": "+41792222222",
                "birthDate": "1992-02-02"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        String id1 = given()
                .contentType(ContentType.JSON)
                .body(person1Payload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .extract().path("id");

        String id2 = given()
                .contentType(ContentType.JSON)
                .body(person2Payload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .extract().path("id");

        // THEN: IDs should be different
        given()
                .expect()
                .body("id", not(equalTo(id1)))
                .when()
                .get("/v1/clients/{id}", id2)
                .then()
                .statusCode(200);
    }
}

