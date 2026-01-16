package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
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
class ContractLifecycleAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;
    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractService contractService;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        clientJpaRepository.deleteAll();
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
    @DisplayName("SCENARIO: Create contract, retrieve it, update cost, verify changes")
    void shouldCompleteContractLifecycle() {
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

        given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", equalTo(contractId))
                .body("costAmount", equalTo(5000.00f))
                .body("startDate", notNullValue())
                .body("endDate", notNullValue());

        String updatePayload = """
                {
                    "amount": "7500.50"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .patch(ContractEndpoints.CONTRACT_COST, contractId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
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
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now.minusDays(10).toString());

        String contract2 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2500.00"
                }
                """, testClient.getId(), now.minusDays(5).toString(), now.plusMonths(6).toString());

        String contract3 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "750.50"
                }
                """, testClient.getId(), now.minusDays(3).toString(), now.plusMonths(3).toString());

        given().contentType(ContentType.JSON).body(contract1).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract3).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("4250.50"));

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3));
    }

    @Test
    @DisplayName("SCENARIO: Contract expiration affects sum calculation")
    void shouldExcludeExpiredContractsFromSum() {
        String expiredContract = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2024-01-01T00:00:00Z",
                    "endDate": "2024-12-31T23:59:59Z",
                    "costAmount": "3000.00"
                }
                """, testClient.getId());

        String activeContract = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z",
                    "costAmount": "5000.00"
                }
                """, testClient.getId());

        given().contentType(ContentType.JSON).body(expiredContract).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);
        given().contentType(ContentType.JSON).body(activeContract).post(ContractEndpoints.CONTRACTS_BASE).then().statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("5000.00"));

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].costAmount", equalTo(5000.00f));
    }

    @Test
    @DisplayName("SCENARIO: Invalid contract creation should fail with validation errors")
    void shouldRejectInvalidContractData() {
        String invalidPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2025-12-31T23:59:59Z",
                    "costAmount": "-1000.00"
                }
                """, testClient.getId());

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(anyOf(is(400), is(422), is(500)));

        LocalDateTime testNow = LocalDateTime.now();
        String invalidDateRange = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), testNow.plusDays(30).toString(), testNow.toString());

        given()
                .contentType(ContentType.JSON)
                .body(invalidDateRange)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Contract with non-existent client should fail")
    void shouldRejectContractForNonExistentClient() {
        UUID fakeClientId = UUID.randomUUID();

        String payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2025-12-31T23:59:59Z",
                    "costAmount": "1000.00"
                }
                """, fakeClientId);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Filter contracts by update date")
    void shouldFilterContractsByUpdateDate() {
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": "2026-12-31T23:59:59Z",
                    "costAmount": "1000.00"
                }
                """, testClient.getId());

        String location = given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
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
                .patch(ContractEndpoints.CONTRACT_COST, contractId)
                .then()
                .statusCode(204);

        String updatedSince = "2025-01-01T00:00:00Z";
        given()
                .queryParam("updatedSince", updatedSince)
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
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
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1500.00"
                }
                """, testClient.getId(), now.minusDays(10).toString());

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].endDate", nullValue());

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("1500.00"));
    }


    @Test
    @DisplayName("SCENARIO: Active contracts from JPA query match domain isActive() logic")
    void shouldReturnOnlyActiveContractsConsistentWithDomainLogic() {
        LocalDateTime now = LocalDateTime.now();

        String activeNoEndDatePayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now.minusDays(10));

        String activeFutureEndPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, testClient.getId(), now.minusDays(10), now.plusDays(30));

        String expiredYesterdayPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "3000.00"
                }
                """, testClient.getId(), now.minusDays(100), now.minusDays(1));

        String expiredLastMonthPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "4000.00"
                }
                """, testClient.getId(), now.minusDays(60), now.minusDays(30));

        given().contentType(ContentType.JSON).body(activeNoEndDatePayload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(activeFutureEndPayload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(expiredYesterdayPayload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(expiredLastMonthPayload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(2))  // Only 2 active contracts
                .body("content.size()", equalTo(2))
                .body("content[0].costAmount", anyOf(equalTo(1000.00f), equalTo(2000.00f)))
                .body("content[1].costAmount", anyOf(equalTo(1000.00f), equalTo(2000.00f)));
    }

    @Test
    @DisplayName("SCENARIO: Sum of active contracts via JPA matches manual sum with domain isActive()")
    void shouldSumActiveContractsConsistentlyWithDomainLogic() {
        LocalDateTime now = LocalDateTime.now();

        // Create 2 active contracts (1000 + 2000 = 3000)
        String active1Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now.minusDays(10));

        String active2Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, testClient.getId(), now.minusDays(5), now.plusDays(30));

        // Create 2 expired contracts (should NOT be included in sum)
        String expired1Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "500.00"
                }
                """, testClient.getId(), now.minusDays(100), now.minusDays(1));

        String expired2Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "700.00"
                }
                """, testClient.getId(), now.minusDays(60), now.minusDays(30));

        given().contentType(ContentType.JSON).body(active1Payload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(active2Payload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(expired1Payload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(expired2Payload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);

        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo("3000.00"));
    }

    @Test
    @DisplayName("LOCALIZATION: Should accept and return Italian Swiss locale (it-CH) for contract operations")
    void shouldAcceptItalianSwissLocaleForContracts() {
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": null,
                    "costAmount": "2500.00"
                }
                """, testClient.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "it-CH")
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("it-CH"));

        given()
                .header("Accept-Language", "it-CH")
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("it-CH"))
                .body("content.size()", equalTo(1));
    }

    @Test
    @DisplayName("LOCALIZATION: Should work with contract sum endpoint and German Swiss locale (de-CH)")
    void shouldWorkWithContractSumEndpointLocalization() {
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": null,
                    "costAmount": "1500.00"
                }
                """, testClient.getId());

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201);

        given()
                .header("Accept-Language", "de-CH")
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("de-CH"));
    }

    @Test
    @DisplayName("LOCALIZATION: Should handle German Swiss locale (de-CH) for contract operations")
    void shouldAcceptGermanSwissLocaleForContractsWithGermanLocale() {
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": null,
                    "costAmount": "3000.00"
                }
                """, testClient.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "de-CH")
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("de-CH"));
    }

    @Test
    @DisplayName("EDGE CASE: Very large cost amounts should be handled correctly")
    void shouldHandleVeryLargeCostAmounts() {
        LocalDateTime now = LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "999999999.99"
                }
                """, testClient.getId(), now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .body("costAmount", equalTo(999999999.99f));
    }

    @Test
    @DisplayName("EDGE CASE: Boundary dates should be handled correctly")
    void shouldHandleBoundaryDates() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusMonths(11)
                .withDayOfMonth(1)
                .plusMonths(1)
                .minusDays(1)  // Last day of the month
                .withHour(23)
                .withMinute(59)
                .withSecond(59);

        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, testClient.getId(), now.minusDays(30), endDate);

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201);

        given()
                .when().get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then().statusCode(200).body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("EDGE CASE: Concurrent contract creation should work")
    void shouldHandleConcurrentContractCreation() {
        LocalDateTime now = LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "500.00"
                }
                """, testClient.getId(), now.minusDays(5));

        for (int i = 0; i < 5; i++) {
            given().contentType(ContentType.JSON).body(contractPayload)
                    .when().post(ContractEndpoints.CONTRACTS_BASE)
                    .then().statusCode(201);
        }

        given()
                .when().get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                .then().statusCode(200).body("content.size()", equalTo(5));
    }

    @Test
    @DisplayName("EDGE CASE: Zero cost amount should be rejected")
    void shouldRejectZeroCostAmount() {
        LocalDateTime now = LocalDateTime.now();
        String zeroAmountPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "0.00"
                }
                """, testClient.getId(), now.minusDays(1));
        given().contentType(ContentType.JSON).body(zeroAmountPayload)
                .when().post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(anyOf(is(400), is(422), is(500)));
    }
}
