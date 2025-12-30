package com.mk.contractservice.integration.usecases;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("UC08: Sum Active Contracts Use Case - Integration Tests")
class ContractSumUseCaseIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Create a test client for contract operations
        testClient = Person.of(
                ClientName.of("Sum Test Client"),
                ClientEmail.of("sum.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        testClient = clientRepository.save(testClient);
    }

    // ========================================
    // CORE BUSINESS RULES
    // ========================================

    @Nested
    @DisplayName("Sum Calculation Rules")
    class SumCalculationRules {

        @Test
        @DisplayName("Should sum ONLY ACTIVE contracts (exclude expired)")
        void shouldSumOnlyActiveContracts() {
            // ========================================
            // GIVEN: 2 active contracts + 2 expired contracts
            // ========================================
            LocalDateTime now = LocalDateTime.now();

            // Active 1: endDate = null (active indefinitely)
            String activeContract1 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": null,
                        "costAmount": "1000.00"
                    }
                    """, now.minusDays(10));

            // Active 2: endDate in the future
            String activeContract2 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "costAmount": "2000.00"
                    }
                    """, now.minusDays(5), now.plusDays(30));

            // Expired 1: endDate in the past (yesterday)
            String expiredContract1 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "costAmount": "500.00"
                    }
                    """, now.minusDays(100), now.minusDays(1));

            // Expired 2: endDate in the past (last month)
            String expiredContract2 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "costAmount": "700.00"
                    }
                    """, now.minusDays(60), now.minusDays(30));

            // Create all contracts
            given().contentType(ContentType.JSON).body(activeContract1)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(activeContract2)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(expiredContract1)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(expiredContract2)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            // ========================================
            // WHEN: Request sum of active contracts
            // ========================================

            // THEN: Sum should be 3000.00 (1000 + 2000)
            // Expired contracts (500 + 700) are NOT included
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(equalTo("3000.00"));
        }

        @Test
        @DisplayName("Should treat NULL endDate as active indefinitely")
        void shouldIncludeContractsWithNullEndDate() {
            // GIVEN: 3 contracts with endDate = null
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < 3; i++) {
                String contractPayload = String.format("""
                        {
                            "startDate": "%s",
                            "endDate": null,
                            "costAmount": "%d.00"
                        }
                        """, now.minusDays(10), (i + 1) * 100);

                given().contentType(ContentType.JSON).body(contractPayload)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then().statusCode(201);
            }

            // WHEN & THEN
            // 100 + 200 + 300 = 600
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(equalTo("600.00"));
        }

        @Test
        @DisplayName("Should return 0 for client with NO active contracts")
        void shouldReturn0WhenNoActiveContracts() {
            // GIVEN
            LocalDateTime now = LocalDateTime.now();

            String expiredContract1 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "costAmount": "1000.00"
                    }
                    """, now.minusMonths(6), now.minusMonths(3));

            String expiredContract2 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "costAmount": "2000.00"
                    }
                    """, now.minusMonths(12), now.minusDays(1));

            given().contentType(ContentType.JSON).body(expiredContract1)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(expiredContract2)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            // WHEN & THEN
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(anyOf(equalTo("0"), equalTo("0.00"), equalTo("0.0")));
        }

        @Test
        @DisplayName("Should return 0 (or 404) for non-existent client")
        void shouldHandleNonExistentClient() {
            // GIVEN
            UUID fakeClientId = UUID.randomUUID();

            // WHEN & THEN
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", fakeClientId)
                    .then()
                    .statusCode(is(404));
        }
    }

    @Nested
    @DisplayName("Data Precision & Large Numbers")
    class DataPrecisionTests {

        @Test
        @DisplayName("Should handle very large sum amounts correctly")
        void shouldHandleVeryLargeAmounts() {
            // GIVEN
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < 5; i++) {
                String contractPayload = String.format("""
                        {
                            "startDate": "%s",
                            "endDate": null,
                            "costAmount": "999999.99"
                        }
                        """, now.minusDays(10));

                given().contentType(ContentType.JSON).body(contractPayload)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then().statusCode(201);
            }

            // WHEN & THEN
            // 5 × 999999.99 = 4999999.95
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(equalTo("4999999.95"));
        }

        @Test
        @DisplayName("Should correctly sum decimal amounts (no rounding errors)")
        void shouldHandleDecimalPrecision() {
            // GIVEN
            LocalDateTime now = LocalDateTime.now();

            String contract1 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": null,
                        "costAmount": "10.50"
                    }
                    """, now.minusDays(10));

            String contract2 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": null,
                        "costAmount": "20.75"
                    }
                    """, now.minusDays(10));

            String contract3 = String.format("""
                    {
                        "startDate": "%s",
                        "endDate": null,
                        "costAmount": "30.33"
                    }
                    """, now.minusDays(10));

            given().contentType(ContentType.JSON).body(contract1)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(contract2)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            given().contentType(ContentType.JSON).body(contract3)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            // WHEN & THEN: 10.50 + 20.75 + 30.33 = 61.58
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(equalTo("61.58"));
        }
    }

    @Nested
    @DisplayName("Performance Requirements")
    class PerformanceRequirements {

        @Test
        @DisplayName("PERFORMANCE: Should respond quickly even with many contracts (< 1 second for 100 contracts)")
        void shouldBePerformantWithManyContracts() {
            // GIVEN
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < 100; i++) {
                String contractPayload = String.format("""
                        {
                            "startDate": "%s",
                            "endDate": null,
                            "costAmount": "100.00"
                        }
                        """, now.minusDays(10));

                given().contentType(ContentType.JSON).body(contractPayload)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then().statusCode(201);
            }
            // WHEN
            long startTime = System.currentTimeMillis();

            final String sum = given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .extract().asString();

            long duration = System.currentTimeMillis() - startTime;

            // THEN
            assertThat(sum).isEqualTo("10000.00");

            assertThat(duration)
                    .as("Sum endpoint should respond in less than 100 ms for 100 contracts")
                    .isLessThan(100);
        }
    }
}

