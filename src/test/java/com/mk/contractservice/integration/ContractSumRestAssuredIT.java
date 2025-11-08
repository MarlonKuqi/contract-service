package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract Sum API Tests - RestAssured")
class ContractSumRestAssuredIT {

    private static final Logger log = LoggerFactory.getLogger(ContractSumRestAssuredIT.class);
    private static final long MAX_CACHE_RESPONSE_TIME_MS = 50L;

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private com.mk.contractservice.application.ContractApplicationService contractApplicationService;

    private Client testClient;


    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        testClient = Person.builder()
                .name(ClientName.of("Jean Dupont"))
                .email(Email.of("jean.dupont." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                .build();
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("GIVEN client with active contracts WHEN GET /sum THEN return correct sum")
    void shouldReturnSumOfActiveContracts() {
        LocalDateTime now = LocalDateTime.now();

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(30), null))
                .costAmount(ContractCost.of(new BigDecimal("1500.50")))
                .build());

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(15), now.plusDays(100)))
                .costAmount(ContractCost.of(new BigDecimal("2500.00")))
                .build());

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(5), now.plusDays(50)))
                .costAmount(ContractCost.of(new BigDecimal("3500.75")))
                .build());

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(equalTo("7501.25"));
    }

    @Test
    @DisplayName("GIVEN client with no contracts WHEN GET /sum THEN return zero")
    void shouldReturnZeroWhenNoContracts() {
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("0"));
    }

    @Test
    @DisplayName("GIVEN client with inactive contracts WHEN GET /sum THEN return zero")
    void shouldReturnZeroForInactiveContracts() {
        LocalDateTime now = LocalDateTime.now();

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(100), now.minusDays(10)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build());

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("0"));
    }

    @Test
    @DisplayName("GIVEN 100 active contracts WHEN GET /sum THEN respond quickly")
    void shouldHandleLargeNumberOfContractsEfficiently() {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal expectedSum = BigDecimal.ZERO;

        for (int i = 0; i < 100; i++) {
            BigDecimal amount = new BigDecimal("100.50");
            expectedSum = expectedSum.add(amount);

            contractRepository.save(Contract.builder()
                    .client(testClient)
                    .period(ContractPeriod.of(now.minusDays(30), now.plusDays(365)))
                    .costAmount(ContractCost.of(amount))
                    .build());
        }

        long startTime = System.currentTimeMillis();
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo(expectedSum.toString()))
                .time(lessThan(300L));
        long firstCallDuration = System.currentTimeMillis() - startTime;
        log.info("First call (cache MISS) for 100 contracts took: {}ms", firstCallDuration);

        startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo(expectedSum.toString()))
                .time(lessThan(MAX_CACHE_RESPONSE_TIME_MS));

        long secondCallDuration = System.currentTimeMillis() - startTime;
        log.info("Second call (cache HIT) for 100 contracts took: {}ms", secondCallDuration);

        startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo(expectedSum.toString()))
                .time(lessThan(MAX_CACHE_RESPONSE_TIME_MS));

        long thirdCallDuration = System.currentTimeMillis() - startTime;
        log.info("Third call (cache HIT) for 100 contracts took: {}ms", thirdCallDuration);

        long totalCachedTime = 0;
        for (int i = 0; i < 20; i++) {
            startTime = System.currentTimeMillis();

            given()
                    .when()
                    .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body(equalTo(expectedSum.toString()))
                    .time(lessThan(MAX_CACHE_RESPONSE_TIME_MS));

            long callDuration = System.currentTimeMillis() - startTime;
            totalCachedTime += callDuration;
            log.debug("Call #{} (cache HIT) took: {}ms", i + 4, callDuration);
        }

        long averageCachedTime = totalCachedTime / 20;
        log.info("\n=== Cache Performance Summary ===");
        log.info("First call (MISS): {}ms", firstCallDuration);
        log.info("Average cached calls (20 calls): {}ms", averageCachedTime);
        log.info("Performance improvement: {}%", (100 - (averageCachedTime * 100 / firstCallDuration)));

        assertThat("Second call should be faster than first call (cache hit)",
                secondCallDuration, lessThan(firstCallDuration));
        assertThat("Third call should be faster than first call (cache hit)",
                thirdCallDuration, lessThan(firstCallDuration));
        assertThat("Average of cached calls should be faster than first call",
                averageCachedTime, lessThan(firstCallDuration));
    }

    @Test
    @DisplayName("GIVEN decimal precision amounts WHEN GET /sum THEN return exact sum")
    void shouldHandleDecimalPrecisionCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(10), now.plusDays(30)))
                .costAmount(ContractCost.of(new BigDecimal("99.99")))
                .build());

        contractRepository.save(Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(now.minusDays(5), now.plusDays(60)))
                .costAmount(ContractCost.of(new BigDecimal("0.01")))
                .build());

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("100.00"));
    }

    @Test
    @DisplayName("GIVEN cached sum WHEN contract created THEN cache invalidated and sum updated")
    void shouldInvalidateCacheWhenContractCreated() {
        LocalDateTime now = LocalDateTime.now();

        contractApplicationService.createForClient(
                testClient.getId(),
                now.minusDays(10),
                now.plusDays(30),
                new BigDecimal("500.00")
        );
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("500.00"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("500.00"))
                .time(lessThan(MAX_CACHE_RESPONSE_TIME_MS));

        contractApplicationService.createForClient(
                testClient.getId(),
                now.minusDays(5),
                now.plusDays(60),
                new BigDecimal("300.00")
        );
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("800.00"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("800.00"))
                .time(lessThan(MAX_CACHE_RESPONSE_TIME_MS));
    }
}

