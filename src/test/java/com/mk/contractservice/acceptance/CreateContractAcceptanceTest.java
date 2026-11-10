package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
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
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testClient = Person.of(
                ClientName.of("Marie Durand"),
                ClientEmail.of("marie.durand." + uniqueId + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20), LocalDate.now(clock))
        );
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with all fields succeeds")
    void shouldCreateContractWithAllFields() {
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "5000.00"
                }
                """, testClient.id(), now, now.plusMonths(12));

        String contractId = given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ContractEndpoints.CONTRACTS_BASE + "/"))
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", notNullValue())
                .body("type", equalTo("LAMAL"))
                .body("costAmount", equalTo(5000.00f))
                .body("startDate", notNullValue())
                .body("endDate", notNullValue())
                .extract()
                .path("id");

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
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.id(), now);

        String contractId = given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ContractEndpoints.CONTRACTS_BASE + "/"))
                .body("id", notNullValue())
                .body("costAmount", equalTo(1000.00f))
                .body("startDate", notNullValue())
                .extract()
                .path("id");

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
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "999999.99"
                }
                """, testClient.id(), now, now.plusYears(5));

        given()
                .config(AcceptanceRestAssured.exactNumbers())
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .body("costAmount", comparesEqualTo(new BigDecimal("999999.99")));
    }

    @Test
    @DisplayName("SCENARIO: Create contract with cost exceeding the storage precision fails")
    void shouldRejectContractWithCostExceedingStoragePrecision() {
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "10000000000.00"
                }
                """, testClient.id(), now, now.plusMonths(6));

        given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with zero cost fails (must be positive non zero)")
    void shouldRejectContractWithZeroCost() {
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "0.00"
                }
                """, testClient.id(), now, now.plusMonths(6));

        given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("invalidContractCost"));
    }

    @Test
    @DisplayName("SCENARIO: Create contract without a body fails with 422 validationError")
    void shouldRejectContractWithoutBody() {
        given()
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"));
    }

    @Test
    @DisplayName("SCENARIO: Create contract for non-existent client fails")
    void shouldRejectContractForNonExistentClient() {
        UUID fakeClientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, fakeClientId, now, now.plusMonths(12));

        given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with negative cost fails")
    void shouldRejectContractWithNegativeCost() {
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "-100.00"
                }
                """, testClient.id(), now, now.plusMonths(12));

        given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("invalidContractCost"));
    }

    @Test
    @DisplayName("SCENARIO: Create contract with endDate before startDate fails")
    void shouldRejectContractWithEndDateBeforeStartDate() {
        LocalDateTime now = LocalDateTime.now(clock);
        String createPayload = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, testClient.id(), now, now.minusDays(10));

        given()
                .body(createPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create contract without required fields fails")
    void shouldRejectContractWithoutRequiredFields() {
        LocalDateTime now = LocalDateTime.now(clock);

        // Missing clientId
        String payloadWithoutClient = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, now, now.plusMonths(12));

        given()
                .body(payloadWithoutClient)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);

        // Missing costAmount
        String payloadWithoutCost = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s"
                }
                """, testClient.id(), now, now.plusMonths(12));

        given()
                .body(payloadWithoutCost)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create multiple contracts for same client succeeds")
    void shouldCreateMultipleContractsForSameClient() {
        LocalDateTime now = LocalDateTime.now(clock);

        String contract1 = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.id(), now.minusDays(10));

        String contract2 = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2500.00"
                }
                """, testClient.id(), now.minusDays(5), now.plusMonths(6));

        String contract3 = String.format("""
                {
                    "clientId": "%s",
                    "type": "LAMAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "750.50"
                }
                """, testClient.id(), now.minusDays(3), now.plusMonths(3));

        given().body(contract1).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().body(contract2).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().body(contract3).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);

        // Verify all contracts are created
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.id())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }

    private Client aCompany() {
        return clientRepository.save(ClientFixtures.newCompany(
                "Acme SA",
                TestDataHelper.uniqueEmail("acme"),
                TestDataHelper.randomSwissPhoneNumber(),
                TestDataHelper.uniqueCompanyIdentifier("CHE")));
    }

    private static String contractOf(final Client client, final String type) {
        return """
                {
                    "clientId": "%s",
                    "type": "%s",
                    "costAmount": "1200.00"
                }
                """.formatted(client.id(), type);
    }

    private void assertHasNoContract(final Client client) {
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", client.id())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LAA", "LPP"})
    @DisplayName("SCENARIO: A company takes out an accident insurance (LAA) or an occupational pension (LPP)")
    void shouldCreateEmployerContractForCompany(final String type) {
        final Client company = aCompany();

        final String contractId = given()
                .body(contractOf(company, type))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .body("type", equalTo(type))
                .extract()
                .path("id");

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("type", equalTo(type));
    }

    @Test
    @DisplayName("SCENARIO: A company cannot take out a health insurance (LAMal), reserved to persons")
    void shouldRefuseLamalForCompany() {
        final Client company = aCompany();

        given()
                .body(contractOf(company, "LAMAL"))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("contractTypeNotAllowed"))
                .body("detail", containsString("LAMAL"))
                .body("detail", containsString("company"));

        assertHasNoContract(company);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LAA", "LPP"})
    @DisplayName("SCENARIO: A person cannot take out an employer insurance (LAA, LPP)")
    void shouldRefuseEmployerContractForPerson(final String type) {
        given()
                .body(contractOf(testClient, type))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("contractTypeNotAllowed"))
                .body("detail", containsString("person"));

        assertHasNoContract(testClient);
    }

    @Test
    @DisplayName("SCENARIO: Create contract without a type fails")
    void shouldRejectContractWithoutType() {
        given()
                .body("""
                        { "clientId": "%s", "costAmount": "1200.00" }
                        """.formatted(testClient.id()))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations[0].field", equalTo("type"));

        assertHasNoContract(testClient);
    }

    @Test
    @DisplayName("SCENARIO: Create contract with an unknown type fails")
    void shouldRejectContractWithUnknownType() {
        given()
                .body(contractOf(testClient, "CASCO"))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("detail", containsString("type"));

        assertHasNoContract(testClient);
    }
}
