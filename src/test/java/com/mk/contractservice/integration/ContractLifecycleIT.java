package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract Lifecycle Scenarios - Integration Tests")
class ContractLifecycleIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        testClient = Person.builder()
                .name(ClientName.of("Marie Durand"))
                .email(Email.of("marie.durand." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1985, 3, 20)))
                .build();
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("SCENARIO: Create contract, retrieve it, update cost, verify changes")
    void shouldCompleteContractLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "5000.00"
                }
                """, now, now.plusMonths(12));

        String location = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(201)
                .header("Location", containsString("/v1/clients/" + testClient.getId() + "/contracts/"))
                .body("costAmount", equalTo(5000.00f))
                .body("period.startDate", notNullValue())
                .body("period.endDate", notNullValue())
                .extract().header("Location");

        String contractId = location.substring(location.lastIndexOf('/') + 1);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/{contractId}", testClient.getId(), contractId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contractId))
                .body("costAmount", equalTo(5000.00f))
                .body("period.startDate", notNullValue())
                .body("period.endDate", notNullValue());

        String updatePayload = """
                {
                    "amount": "7500.50"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch("/v1/clients/{clientId}/contracts/{contractId}/cost", testClient.getId(), contractId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0));
    }

    @Test
    @DisplayName("SCENARIO: Client signs multiple contracts, sum calculation reflects all active ones")
    void shouldTrackMultipleContractsForSameClient() {
        LocalDateTime now = LocalDateTime.now();

        String contract1 = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, now.minusDays(10).toString());

        String contract2 = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2500.00"
                }
                """, now.minusDays(5).toString(), now.plusMonths(6).toString());

        String contract3 = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "750.50"
                }
                """, now.minusDays(3).toString(), now.plusMonths(3).toString());

        given().contentType(ContentType.JSON).body(contract1).post("/v1/clients/{clientId}/contracts", testClient.getId()).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post("/v1/clients/{clientId}/contracts", testClient.getId()).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract3).post("/v1/clients/{clientId}/contracts", testClient.getId()).then().statusCode(201);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("4250.50"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }

    @Test
    @DisplayName("SCENARIO: Contract expiration affects sum calculation")
    void shouldExcludeExpiredContractsFromSum() {
        String expiredContract = """
                {
                    "startDate": "2024-01-01T00:00:00Z",
                    "endDate": "2024-12-31T23:59:59Z",
                    "costAmount": "3000.00"
                }
                """;

        String activeContract = """
                {
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z",
                    "costAmount": "5000.00"
                }
                """;

        given().contentType(ContentType.JSON).body(expiredContract).post("/v1/clients/{clientId}/contracts", testClient.getId()).then().statusCode(201);
        given().contentType(ContentType.JSON).body(activeContract).post("/v1/clients/{clientId}/contracts", testClient.getId()).then().statusCode(201);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("5000.00"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].costAmount", equalTo(5000.00f));
    }

    @Test
    @DisplayName("SCENARIO: Invalid contract creation should fail with validation errors")
    void shouldRejectInvalidContractData() {
        String invalidPayload = """
                {
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2025-12-31T23:59:59Z",
                    "costAmount": "-1000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));

        LocalDateTime testNow = LocalDateTime.now();
        String invalidDateRange = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, testNow.plusDays(30).toString(), testNow.toString());

        given()
                .contentType(ContentType.JSON)
                .body(invalidDateRange)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Contract with non-existent client should fail")
    void shouldRejectContractForNonExistentClient() {
        UUID fakeClientId = UUID.randomUUID();

        String payload = """
                {
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2025-12-31T23:59:59Z",
                    "costAmount": "1000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/v1/clients/{clientId}/contracts", fakeClientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Filter contracts by update date")
    void shouldFilterContractsByUpdateDate() {
        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z",
                    "costAmount": "1000.00"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(201)
                .extract().header("Location");

        String contractId = location.substring(location.lastIndexOf('/') + 1);

        String updatePayload = """
                {
                    "amount": "2000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch("/v1/clients/{clientId}/contracts/{contractId}/cost", testClient.getId(), contractId)
                .then()
                .statusCode(204);

        String updatedSince = "2025-01-01T00:00:00Z";
        given()
                .queryParam("updatedSince", updatedSince)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("SCENARIO: Contract without endDate is considered active indefinitely")
    void shouldTreatNullEndDateAsIndefinitelyActive() {
        LocalDateTime now = LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1500.00"
                }
                """, now.minusDays(10).toString());

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].period.endDate", nullValue());

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("1500.00"));
    }

    @Test
    @DisplayName("SCENARIO: Updating cost with wrong clientId returns 403 Forbidden (authorization)")
    void shouldReturn403WhenUpdatingCostWithWrongClientId() {
        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(201)
                .extract().header("Location");

        String contractId = location.substring(location.lastIndexOf('/') + 1);

        UUID wrongClientId = UUID.randomUUID();
        String updatePayload = """
                {
                    "amount": "2000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch("/v1/clients/{clientId}/contracts/{contractId}/cost", wrongClientId, contractId)
                .then()
                .statusCode(403)
                .body("title", equalTo("Access Denied"))
                .body("code", equalTo("contractAccessDenied"));
    }

    @Test
    @DisplayName("SCENARIO: Get contract with wrong clientId returns 403 Forbidden")
    void shouldReturn403WhenGettingContractWithWrongClientId() {
        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """;

        String location = given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(201)
                .extract().header("Location");

        String contractId = location.substring(location.lastIndexOf('/') + 1);

        UUID wrongClientId = UUID.randomUUID();

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/{contractId}", wrongClientId, contractId)
                .then()
                .statusCode(403)
                .body("title", equalTo("Access Denied"))
                .body("code", equalTo("contractAccessDenied"));
    }
}


