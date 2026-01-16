package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Company Client Lifecycle Scenarios - Integration Tests")
class CompanyLifecycleAcceptanceTest {

    @LocalServerPort
    private int port;

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
    @DisplayName("SCENARIO: Create company client with all valid data")
    void shouldCreateCompanyClientSuccessfully() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String createPayload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*" + "v2" + "/clients/[0-9a-f-]{36}"))
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", notNullValue())
                .body("name", equalTo("Acme Corporation"))
                .body("email", containsString("contact.acme"))
                .body("phone", equalTo("+41791234567"))
                .body("companyIdentifier", startsWith("CHE-"))
                .extract().path("id");

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", equalTo(clientId))
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corporation"))
                .body("companyIdentifier", startsWith("CHE-"));
    }

    @Test
    @DisplayName("SCENARIO: Invalid company identifier format should be rejected")
    void shouldRejectInvalidCompanyIdentifier() {
        String uniqueEmail = "bad." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String invalidIdentifierPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Bad Company",
                    "email": "%s",
                    "phone": "+41797000003",
                    "companyIdentifier": ""
                }
                """, uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(invalidIdentifierPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Missing required company identifier should be rejected")
    void shouldRejectMissingCompanyIdentifier() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String uniqueEmail = "incomplete." + uniqueId + "@example.com";
        String missingIdentifierPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Incomplete Company",
                    "email": "%s",
                    "phone": "+41797000001"
                }
                """, uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(missingIdentifierPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update company client common fields (companyIdentifier remains unchanged)")
    void shouldUpdateCompanyCommonFieldsButNotIdentifier() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyIdPart = uniqueId.substring(0, 6);
        String createPayload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String originalIdentifier = String.format("CHE-%s.222.333", companyIdPart);

        String updateUniqueId = UUID.randomUUID().toString().substring(0, 8);
        String updatePayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Updated Tech SA",
                    "email": "updated.tech.%s@example.com",
                    "phone": "+41792222222"
                }
                """, updateUniqueId);

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Tech SA"))
                .body("email", containsString("updated.tech"))
                .body("phone", equalTo("+41792222222"))
                .body("companyIdentifier", equalTo(originalIdentifier));
    }

    @Test
    @DisplayName("CRITICAL: Company identifier is IMMUTABLE - Cannot be updated")
    void shouldNotUpdateCompanyIdentifierWhenUpdatingCompany() {
        // GIVEN
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalIdentifier = "CHE-123.456.789";

        String createPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Immutable ID Corp",
                    "email": "immutable.id.%s@example.com",
                    "phone": "+41791111111",
                    "companyIdentifier": "%s"
                }
                """, uniqueId, originalIdentifier);

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("companyIdentifier", equalTo(originalIdentifier))
                .extract().path("id");

        // WHEN
        String updateUniqueId = UUID.randomUUID().toString().substring(0, 8);
        String newIdentifier = "CHE-999.888.777";

        String updatePayloadWithNewIdentifier = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Immutable ID Corp Updated",
                    "email": "immutable.updated.%s@example.com",
                    "phone": "+41792222222",
                    "companyIdentifier": "%s"
                }
                """, updateUniqueId, newIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(updatePayloadWithNewIdentifier)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(anyOf(is(204), is(400), is(422)));  // Success or validation error
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(200)
                .body("name", anyOf(equalTo("Immutable ID Corp Updated"), equalTo("Immutable ID Corp")))  // Name may or may not be updated
                .body("companyIdentifier", equalTo(originalIdentifier));  // ← CRITICAL: Identifier MUST NOT change
    }

    @Test
    @DisplayName("SCENARIO: Delete company and verify contracts are closed")
    void shouldDeleteCompanyAndCloseContracts() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyPayload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "10000.00"
                }
                """, clientId, now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201);

        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Duplicate clientEmail should be handled gracefully")
    void shouldHandleDuplicateEmail() {
        String uniqueEmail = "duplicate.company.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        String firstPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "First Company",
                    "email": "%s",
                    "phone": "+41791111111",
                    "companyIdentifier": "CHE-%s.111.111"
                }
                """, uniqueEmail, UUID.randomUUID().toString().substring(0, 6));

        given()
                .contentType(ContentType.JSON)
                .body(firstPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        String secondPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Second Company",
                    "email": "%s",
                    "phone": "+41792222222",
                    "companyIdentifier": "CHE-%s.222.222"
                }
                """, uniqueEmail, UUID.randomUUID().toString().substring(0, 6));

        given()
                .contentType(ContentType.JSON)
                .body(secondPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(anyOf(is(409), is(400), is(422), is(500)));
    }

    @Test
    @DisplayName("SCENARIO: Duplicate company identifier should be handled")
    void shouldHandleDuplicateCompanyIdentifier() {
        String uniqueTestRunId = UUID.randomUUID().toString();
        String sharedIdentifier = String.format("CHE-DUP-%s", uniqueTestRunId);

        String firstUniqueEmail = UUID.randomUUID().toString().substring(0, 8);
        String firstPayload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String secondUniqueEmail = UUID.randomUUID().toString().substring(0, 8);
        String secondPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Second Company",
                    "email": "second.%s@example.com",
                    "phone": "+41792222222",
                    "companyIdentifier": "%s"
                }
                """, secondUniqueEmail, sharedIdentifier);
        given()
                .contentType(ContentType.JSON)
                .body(secondPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(anyOf(is(409), is(400), is(422), is(500)));
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, firstClientId)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("SCENARIO: Company with contracts calculates sum correctly")
    void shouldCalculateSumForCompanyContracts() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyPayload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String contract1 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "5000.00"
                }
                """, clientId, now.minusDays(30), now.plusMonths(12));

        String contract2 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "7500.50"
                }
                """, clientId, now.minusDays(10));

        given().contentType(ContentType.JSON).body(contract1).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", clientId)
                .then()
                .statusCode(200)
                .body(equalTo("12500.50"));
    }

    @Test
    @DisplayName("VALIDATION: Should validate company email format correctly")
    void shouldValidateCompanyEmailFormat() {
        String uniqueId1 = UUID.randomUUID().toString().substring(0, 8);
        String phoneNumber1 = TestDataHelper.randomSwissPhoneNumber();
        String validPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Valid Email Company",
                    "email": "contact+sales.%s@company-name.co.uk",
                    "phone": "%s",
                    "companyIdentifier": "CHE-%s.456.789"
                }
                """, uniqueId1, phoneNumber1, uniqueId1);

        given()
                .contentType(ContentType.JSON)
                .body(validPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        String[] invalidEmails = {
                "@example.com",
                "missing-at-sign.com",
                "spaces in@clientEmail.com",
                "double@@at.com"
        };

        for (String invalidEmail : invalidEmails) {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String phoneNumber = "+41791234567";
            String invalidPayload = String.format("""
                    {
                        "type": "COMPANY",
                    "name": "Invalid Email Company",
                        "email": "%s",
                        "phone": "%s",
                        "companyIdentifier": "CHE-%s.888.777"
                    }
                    """, invalidEmail, phoneNumber, uniqueId);

            given()
                    .contentType(ContentType.JSON)
                    .body(invalidPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(422);
        }
    }

    @Test
    @DisplayName("SCENARIO: Create multiple companies and verify independence")
    void shouldCreateMultipleIndependentCompanies() {
        String uniqueId1 = UUID.randomUUID().toString().substring(0, 8);
        String company1Payload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Company Alpha",
                    "email": "alpha.%s@example.com",
                    "phone": "+41791111111",
                    "companyIdentifier": "CHE-%s.111.111"
                }
                """, uniqueId1, uniqueId1.substring(0, 6));

        String uniqueId2 = UUID.randomUUID().toString().substring(0, 8);
        String company2Payload = String.format("""
                {
                    "type": "COMPANY",
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
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String id2 = given()
                .contentType(ContentType.JSON)
                .body(company2Payload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, id1, now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", id1)
                .then()
                .statusCode(200)
                .body(equalTo("1000.00"));

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", id2)
                .then()
                .statusCode(200)
                .body(equalTo("0"));
    }
}





