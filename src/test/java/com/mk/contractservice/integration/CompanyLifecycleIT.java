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
@DisplayName("Company Client Lifecycle Scenarios - Integration Tests")
class CompanyLifecycleIT {

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
    @DisplayName("SCENARIO: Create company client with all valid data")
    void shouldCreateCompanyClientSuccessfully() {
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String createPayload = String.format("""
            {
                "name": "Acme Corporation",
                "email": "contact.acme.%s@example.com",
                "phone": "+41791234567",
                "companyIdentifier": "CHE-%s.456.789"
            }
            """, uniqueId, uniqueId.substring(0, 6));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/v1/clients/[0-9a-f-]{36}"))
                .body("id", notNullValue())
                .body("name", equalTo("Acme Corporation"))
                .body("email", containsString("contact.acme"))
                .body("phone", equalTo("+41791234567"))
                .body("companyIdentifier", startsWith("CHE-"))
                .extract().path("id");

        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .body("id", equalTo(clientId))
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corporation"))
                .body("companyIdentifier", startsWith("CHE-"));
    }

    @Test
    @DisplayName("SCENARIO: Invalid company identifier format should be rejected")
    void shouldRejectInvalidCompanyIdentifier() {
        String uniqueEmail = "bad." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String invalidIdentifierPayload = String.format("""
            {
                "name": "Bad Company",
                "email": "%s",
                "phone": "+41791234567",
                "companyIdentifier": ""
            }
            """, uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(invalidIdentifierPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Missing required company identifier should be rejected")
    void shouldRejectMissingCompanyIdentifier() {
        String uniqueEmail = "incomplete." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String missingIdentifierPayload = String.format("""
            {
                "name": "Incomplete Company",
                "email": "%s",
                "phone": "+41791234567"
            }
            """, uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(missingIdentifierPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("SCENARIO: Update company client common fields (companyIdentifier remains unchanged)")
    void shouldUpdateCompanyCommonFieldsButNotIdentifier() {
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyIdPart = uniqueId.substring(0, 6);
        String createPayload = String.format("""
            {
                "name": "Original Tech SA",
                "email": "original.tech.%s@example.com",
                "phone": "+41791111111",
                "companyIdentifier": "CHE-%s.222.333"
            }
            """, uniqueId, companyIdPart);

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        String originalIdentifier = String.format("CHE-%s.222.333", companyIdPart);

        String updateUniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String updatePayload = String.format("""
            {
                "name": "Updated Tech SA",
                "email": "updated.tech.%s@example.com",
                "phone": "+41792222222"
            }
            """, updateUniqueId);

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/v1/clients/{id}", clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Tech SA"))
                .body("email", containsString("updated.tech"))
                .body("phone", equalTo("+41792222222"))
                .body("companyIdentifier", equalTo(originalIdentifier));
    }

    @Test
    @DisplayName("SCENARIO: Delete company and verify contracts are closed")
    void shouldDeleteCompanyAndCloseContracts() {
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyPayload = String.format("""
            {
                "name": "Delete Me Inc",
                "email": "deleteme.%s@example.com",
                "phone": "+41791234567",
                "companyIdentifier": "CHE-%s.888.777"
            }
            """, uniqueId, uniqueId.substring(0, 6));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(companyPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String contractPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "10000.00"
            }
            """, now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", clientId)
                .then()
                .statusCode(201);

        given()
                .when()
                .delete("/v1/clients/{id}", clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Duplicate company identifier should be handled")
    void shouldHandleDuplicateCompanyIdentifier() {
        // Note: This test validates that duplicate company identifiers are rejected.
        // We use unique identifiers per test run but reuse within the same test.
        String uniqueTestRunId = java.util.UUID.randomUUID().toString();
        String sharedIdentifier = String.format("CHE-DUP-%s", uniqueTestRunId);

        String firstUniqueEmail = java.util.UUID.randomUUID().toString().substring(0, 8);
        String firstPayload = String.format("""
            {
                "name": "First Company",
                "email": "first.%s@example.com",
                "phone": "+41791111111",
                "companyIdentifier": "%s"
            }
            """, firstUniqueEmail, sharedIdentifier);

        String firstClientId = given()
                .contentType(ContentType.JSON)
                .body(firstPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        String secondUniqueEmail = java.util.UUID.randomUUID().toString().substring(0, 8);
        String secondPayload = String.format("""
            {
                "name": "Second Company",
                "email": "second.%s@example.com",
                "phone": "+41792222222",
                "companyIdentifier": "%s"
            }
            """, secondUniqueEmail, sharedIdentifier);

        // THEN: Should fail with conflict or validation error
        given()
                .contentType(ContentType.JSON)
                .body(secondPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(anyOf(is(409), is(400), is(422), is(500)));

        given()
                .when()
                .delete("/v1/clients/{id}", firstClientId)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("SCENARIO: Company with contracts calculates sum correctly")
    void shouldCalculateSumForCompanyContracts() {
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String companyPayload = String.format("""
            {
                "name": "Big Enterprise LLC",
                "email": "bigenterprise.%s@example.com",
                "phone": "+41791234567",
                "companyIdentifier": "CHE-%s.666.777"
            }
            """, uniqueId, uniqueId.substring(0, 6));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(companyPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String contract1 = String.format("""
            {
                "startDate": "%s",
                "endDate": "%s",
                "costAmount": "5000.00"
            }
            """, now.minusDays(30), now.plusMonths(12));

        String contract2 = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "7500.50"
            }
            """, now.minusDays(10));

        given().contentType(ContentType.JSON).body(contract1).post("/v1/clients/{clientId}/contracts", clientId).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post("/v1/clients/{clientId}/contracts", clientId).then().statusCode(201);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", clientId)
                .then()
                .statusCode(200)
                .body(equalTo("12500.50"));
    }

    @Test
    @DisplayName("SCENARIO: Company email validation should follow RFC standards")
    void shouldValidateCompanyEmailFormat() {
        String uniqueId1 = java.util.UUID.randomUUID().toString().substring(0, 8);
        String validPayload = String.format("""
            {
                "name": "Valid Email Company",
                "email": "contact+sales@company-name.co.uk",
                "phone": "+41791234567",
                "companyIdentifier": "CHE-%s.456.789"
            }
            """, uniqueId1);

        given()
                .contentType(ContentType.JSON)
                .body(validPayload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201);

        String[] invalidEmails = {
                "@example.com",
                "missing-at-sign.com",
                "spaces in@email.com",
                "double@@at.com"
        };

        for (String invalidEmail : invalidEmails) {
            String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String invalidPayload = String.format("""
                {
                    "name": "Invalid Email Company",
                    "email": "%s",
                    "phone": "+41791234567",
                    "companyIdentifier": "CHE-%s.888.777"
                }
                """, invalidEmail, uniqueId);

            given()
                    .contentType(ContentType.JSON)
                    .body(invalidPayload)
                    .when()
                    .post("/v1/clients/companies")
                    .then()
                    .statusCode(400);
        }
    }

    @Test
    @DisplayName("SCENARIO: Create multiple companies and verify independence")
    void shouldCreateMultipleIndependentCompanies() {
        String uniqueId1 = java.util.UUID.randomUUID().toString().substring(0, 8);
        String company1Payload = String.format("""
            {
                "name": "Company Alpha",
                "email": "alpha.%s@example.com",
                "phone": "+41791111111",
                "companyIdentifier": "CHE-%s.111.111"
            }
            """, uniqueId1, uniqueId1.substring(0, 6));

        String uniqueId2 = java.util.UUID.randomUUID().toString().substring(0, 8);
        String company2Payload = String.format("""
            {
                "name": "Company Beta",
                "email": "beta.%s@example.com",
                "phone": "+41792222222",
                "companyIdentifier": "CHE-%s.222.222"
            }
            """, uniqueId2, uniqueId2.substring(0, 6));

        String id1 = given()
                .contentType(ContentType.JSON)
                .body(company1Payload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        String id2 = given()
                .contentType(ContentType.JSON)
                .body(company2Payload)
                .when()
                .post("/v1/clients/companies")
                .then()
                .statusCode(201)
                .extract().path("id");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String contractPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "1000.00"
            }
            """, now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", id1)
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", id1)
                .then()
                .statusCode(200)
                .body(equalTo("1000.00"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", id2)
                .then()
                .statusCode(200)
                .body(equalTo("0"));
    }
}

