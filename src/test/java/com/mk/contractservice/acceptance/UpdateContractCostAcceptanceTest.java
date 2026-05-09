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
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Update Contract Cost - Acceptance Tests")
class UpdateContractCostAcceptanceTest {

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
    @DisplayName("SCENARIO: Update contract cost succeeds and persists changes")
    void shouldUpdateContractCost() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("5000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "7500.50"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        // Verify the cost was updated
        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("costAmount", equalTo(7500.50f));
    }

    @Test
    @DisplayName("SCENARIO: Increase contract cost succeeds")
    void shouldIncreaseContractCost() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "2000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("costAmount", equalTo(2000.00f));
    }

    @Test
    @DisplayName("SCENARIO: Decrease contract cost succeeds")
    void shouldDecreaseContractCost() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("5000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "3000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("costAmount", equalTo(3000.00f));
    }

    @Test
    @DisplayName("SCENARIO: Update contract cost to zero fails (must be positive non zero)")
    void shouldRejectZeroCost() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "0.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update cost for non-existent contract returns 404")
    void shouldReturn404WhenUpdatingNonExistentContract() {
        UUID fakeId = UUID.randomUUID();

        String updatePayload = """
                {
                    "amount": "1000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update contract cost with negative amount fails")
    void shouldRejectNegativeAmount() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "-100.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update contract cost with invalid format fails")
    void shouldRejectInvalidCostFormat() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        String updatePayload = """
                {
                    "amount": "invalid"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update contract cost multiple times succeeds")
    void shouldAllowMultipleUpdates() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        // First update
        String update1 = """
                {
                    "amount": "2000.00"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(update1)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        // Second update
        String update2 = """
                {
                    "amount": "3000.00"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(update2)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        // Third update
        String update3 = """
                {
                    "amount": "1500.00"
                }
                """;
        given()
                .contentType(ContentType.JSON)
                .body(update3)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, savedContract.getId())
                .then()
                .statusCode(204);

        // Verify final cost
        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("costAmount", equalTo(1500.00f));
    }
}
