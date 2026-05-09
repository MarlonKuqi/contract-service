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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Read Contract - Acceptance Tests")
class ReadContractAcceptanceTest {

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
    @DisplayName("SCENARIO: Read contract with all fields returns correct data")
    void shouldReadContractWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, now.plusMonths(12)))
                .costAmount(ContractCost.of(new BigDecimal("5000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", equalTo(savedContract.getId().toString()))
                .body("clientId", equalTo(testClient.getId().toString()))
                .body("costAmount", equalTo(5000.00f))
                .body("startDate", notNullValue())
                .body("endDate", notNullValue())
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("SCENARIO: Read open-ended contract returns null endDate")
    void shouldReadOpenEndedContract() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now, null))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(savedContract.getId().toString()))
                .body("endDate", equalTo(null))
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("SCENARIO: Read expired contract returns active false")
    void shouldReadExpiredContract() {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = Contract.builder()
                .clientId(testClient.getId())
                .period(ContractPeriod.of(now.minusMonths(12), now.minusDays(1)))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract savedContract = contractRepository.save(contract);

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, savedContract.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(savedContract.getId().toString()))
                .body("active", equalTo(false));
    }

    @Test
    @DisplayName("SCENARIO: Read non-existent contract returns 404")
    void shouldReturn404ForNonExistentContract() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Read contract with invalid UUID format returns 400")
    void shouldReturn400ForInvalidUuidFormat() {
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "/invalid-uuid")
                .then()
                .statusCode(400);
    }
}
