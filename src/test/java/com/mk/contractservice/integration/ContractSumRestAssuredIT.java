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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract Sum API Tests - RestAssured")
class ContractSumRestAssuredIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        testClient = new Person(
                ClientName.of("Jean Dupont"),
                Email.of("jean.dupont." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("GIVEN client with active contracts WHEN GET /sum THEN return correct sum")
    void shouldReturnSumOfActiveContracts() {
        LocalDateTime now = LocalDateTime.now();

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(30), null),
                ContractCost.of(new BigDecimal("1500.50"))
        ));

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(15), now.plusDays(100)),
                ContractCost.of(new BigDecimal("2500.00"))
        ));

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(5), now.plusDays(50)),
                ContractCost.of(new BigDecimal("3500.75"))
        ));

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

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(100), now.minusDays(10)),
                ContractCost.of(new BigDecimal("1000.00"))
        ));

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

            contractRepository.save(new Contract(
                    testClient,
                    ContractPeriod.of(now.minusDays(30), now.plusDays(365)),
                    ContractCost.of(amount)
            ));
        }

        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo(expectedSum.toString()))
                .time(lessThan(1000L)); // 1 second max

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Sum calculation for 100 contracts took: " + duration + "ms");
    }

    @Test
    @DisplayName("GIVEN decimal precision amounts WHEN GET /sum THEN return exact sum")
    void shouldHandleDecimalPrecisionCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(10), now.plusDays(30)),
                ContractCost.of(new BigDecimal("99.99"))
        ));

        contractRepository.save(new Contract(
                testClient,
                ContractPeriod.of(now.minusDays(5), now.plusDays(60)),
                ContractCost.of(new BigDecimal("0.01"))
        ));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("100.00"));
    }
}


