package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Create Contract - Acceptance Tests")
class CreateContractAcceptanceTest {

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

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testClient = Person.of(
                ClientName.of("Marie Durand"),
                ClientEmail.of("marie.durand." + uniqueId + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with all fields succeeds")
    void shouldCreateContractWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "5000.00"
                }
                """, testClient.getId(), now, now.plusMonths(12));

        String contractId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ContractEndpoints.CONTRACTS_BASE + "/"))
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", notNullValue())
                .body("costAmount", equalTo(5000.00f))
                .body("startDate", notNullValue())
                .body("endDate", notNullValue())
                .extract().path("id");

        // Verify persistence
        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contractId))
                .body("costAmount", equalTo(5000.00f));
    }

    @Test
    @DisplayName("SCENARIO: Create contract with null endDate (open-ended) succeeds")
    void shouldCreateOpenEndedContract() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now);

        String contractId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ContractEndpoints.CONTRACTS_BASE + "/"))
                .body("id", notNullValue())
                .body("costAmount", equalTo(1000.00f))
                .body("startDate", notNullValue())
                .extract().path("id");

        // Verify persistence
        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contractId))
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("SCENARIO: Create contract with very high cost succeeds")
    void shouldCreateContractWithHighCost() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "999999.99"
                }
                """, testClient.getId(), now, now.plusYears(5));

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .body("costAmount", equalTo(999999.99f));
    }

    @Test
    @DisplayName("SCENARIO: Create contract with zero cost fails (must be positive non zero)")
    void shouldRejectContractWithZeroCost() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "0.00"
                }
                """, testClient.getId(), now, now.plusMonths(6));

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create contract for non-existent client fails")
    void shouldRejectContractForNonExistentClient() {
        UUID fakeClientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, fakeClientId, now, now.plusMonths(12));

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with negative cost fails")
    void shouldRejectContractWithNegativeCost() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "-100.00"
                }
                """, testClient.getId(), now, now.plusMonths(12));

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with endDate before startDate fails")
    void shouldRejectContractWithEndDateBeforeStartDate() {
        LocalDateTime now = LocalDateTime.now();
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now, now.minusDays(10));

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create contract without required fields fails")
    void shouldRejectContractWithoutRequiredFields() {
        LocalDateTime now = LocalDateTime.now();

        // Missing clientId
        String payloadWithoutClient = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, now, now.plusMonths(12));

        given()
                .contentType(ContentType.JSON)
                .body(payloadWithoutClient)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);

        // Missing costAmount
        String payloadWithoutCost = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s"
                }
                """, testClient.getId(), now, now.plusMonths(12));

        given()
                .contentType(ContentType.JSON)
                .body(payloadWithoutCost)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create multiple contracts for same client succeeds")
    void shouldCreateMultipleContractsForSameClient() {
        LocalDateTime now = LocalDateTime.now();

        String contract1 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now.minusDays(10));

        String contract2 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2500.00"
                }
                """, testClient.getId(), now.minusDays(5), now.plusMonths(6));

        String contract3 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "750.50"
                }
                """, testClient.getId(), now.minusDays(3), now.plusMonths(3));

        given().contentType(ContentType.JSON).body(contract1).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract3).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);

        // Verify all contracts are created
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }
}
