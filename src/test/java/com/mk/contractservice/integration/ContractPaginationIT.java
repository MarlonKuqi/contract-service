package com.mk.contractservice.integration;

import com.mk.contractservice.web.controller.ClientController;
import com.mk.contractservice.web.controller.ContractController;

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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract Pagination Integration Tests")
class ContractPaginationIT {

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
        RestAssured.port = port;

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        testClient = Person.builder()
                .name(ClientName.of("John Doe"))
                .email(Email.of("john.pagination@test.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build();
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("Should navigate across multiple pages with default page size")
    void shouldPaginateContractsAcrossMultiplePages() {
        final int pageSize = 10;
        for (int i = 1; i <= 25; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .when()
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(10))
                .body("pageNumber", equalTo(0))
                .body("pageSize", equalTo(pageSize))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(3))
                .body("first", equalTo(true))
                .body("last", equalTo(false));

        given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(10))
                .body("pageNumber", equalTo(1))
                .body("pageSize", equalTo(10))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(3))
                .body("first", equalTo(false))
                .body("last", equalTo(false));

        given()
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("pageNumber", equalTo(2))
                .body("pageSize", equalTo(pageSize))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(3))
                .body("first", equalTo(false))
                .body("last", equalTo(true));
    }

    @Test
    @DisplayName("Should return empty page when requesting page beyond available data")
    void shouldReturnEmptyPageWhenBeyondAvailableData() {
        for (int i = 1; i <= 5; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("page", 5)
                .queryParam("size", 10)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("pageNumber", equalTo(5))
                .body("pageSize", equalTo(10))
                .body("totalElements", equalTo(5))
                .body("totalPages", equalTo(1))
                .body("first", equalTo(false))
                .body("last", equalTo(true));
    }

    @Test
    @DisplayName("Should sort contracts by lastModified descending")
    void shouldSortContractsByLastModifiedDescending() {
        LocalDateTime now = LocalDateTime.now();

        String oldContract = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "100.00"
                }
                """, now.minusDays(10));

        String middleContract = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "200.00"
                }
                """, now.minusDays(5));

        String recentContract = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "300.00"
                }
                """, now.minusDays(1));

        given().contentType(ContentType.JSON).body(middleContract)
                .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then().statusCode(201);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given().contentType(ContentType.JSON).body(oldContract)
                .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then().statusCode(201);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given().contentType(ContentType.JSON).body(recentContract)
                .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then().statusCode(201);

        given()
                .queryParam("sort", "lastModified,desc")
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("content[0].costAmount", equalTo(300.00f))
                .body("content[1].costAmount", equalTo(100.00f))
                .body("content[2].costAmount", equalTo(200.00f));
    }

    @Test
    @DisplayName("Should combine pagination with updatedSince filter")
    void shouldFilterAndPaginateByUpdatedSince() {
        LocalDateTime filterDate = LocalDateTime.now().minusDays(3);

        for (int i = 1; i <= 5; i++) {
            String oldContract = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given().contentType(ContentType.JSON).body(oldContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 6; i <= 15; i++) {
            String recentContract = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given().contentType(ContentType.JSON).body(recentContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        given()
                .queryParam("updatedSince", filterDate.toString())
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("totalElements", greaterThanOrEqualTo(10))
                .body("totalPages", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Should use default page size when not specified")
    void shouldUseDefaultPageSizeWhenNotSpecified() {
        for (int i = 1; i <= 25; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(20))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(25));
    }

    @Test
    @DisplayName("Should handle different page sizes correctly")
    void shouldHandleDifferentPageSizes() {
        for (int i = 1; i <= 50; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 5)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("pageSize", equalTo(5))
                .body("totalPages", equalTo(10));

        given()
                .queryParam("size", 25)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(25))
                .body("pageSize", equalTo(25))
                .body("totalPages", equalTo(2));

        given()
                .queryParam("size", 50)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(50))
                .body("pageSize", equalTo(50))
                .body("totalPages", equalTo(1))
                .body("first", equalTo(true))
                .body("last", equalTo(true));
    }

    @Test
    @DisplayName("Should return correct metadata for single page result")
    void shouldReturnCorrectMetadataForSinglePage() {
        for (int i = 1; i <= 3; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 20)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("pageNumber", equalTo(0))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(1))
                .body("first", equalTo(true))
                .body("last", equalTo(true));
    }

    @Test
    @DisplayName("Should return empty page when client has no contracts")
    void shouldReturnEmptyPageWhenNoContracts() {

        given()
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("pageNumber", equalTo(0))
                .body("totalElements", equalTo(0))
                .body("totalPages", equalTo(0))
                .body("first", equalTo(true))
                .body("last", equalTo(true));
    }

    @Test
    @DisplayName("Should only return active contracts in pagination")
    void shouldOnlyReturnActiveContractsInPagination() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            String expiredContract = String.format("""
                    {
                        "startDate": "2024-01-01T00:00:00",
                        "endDate": "%s",
                        "costAmount": "%d.00"
                    }
                    """, now.minusDays(1), i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(expiredContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        for (int i = 6; i <= 15; i++) {
            String activeContract = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(activeContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 5)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("totalElements", equalTo(10))
                .body("totalPages", equalTo(2));
    }

    @Test
    @DisplayName("LOCALIZATION: Should accept and return Italian Swiss locale (it-CH) for contract operations")
    void shouldAcceptItalianSwissLocaleForContracts() {
        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00",
                    "endDate": null,
                    "costAmount": "2500.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "it-CH")
                .body(contractPayload)
                .when()
                .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("it-CH"));

        given()
                .header("Accept-Language", "it-CH")
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("it-CH"));
    }

    @Test
    @DisplayName("LOCALIZATION: Should work with contract sum endpoint")
    void shouldWorkWithContractSumEndpointLocalization() {
        given()
                .header("Accept-Language", "de-CH")
                .when()
                .get(ContractController.PATH_BASE + ContractController.PATH_SUM + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("de-CH"));
    }

    void shouldEnforceMaxPageSizeLimit() {
        for (int i = 1; i <= 50; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%d.00"
                    }
                    """, i * 100);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }
        given()
                .queryParam("size", 200)
                .when()
                .get(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(50))
                .body("pageSize", equalTo(100));
    }


    @Test
    @DisplayName("Sum endpoint should be performant with 100 contracts (< 100ms)")
    void sumShouldBePerformantWith100Contracts() {
        for (int i = 1; i <= 100; i++) {
            String contractPayload = String.format("""
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "1000.00"
                    }
                    """);

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        long startTime = System.currentTimeMillis();

        String sum = given()
                .when()
                .get(ContractController.PATH_BASE + ContractController.PATH_SUM + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .extract()
                .asString();

        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
                .as("Sum of 100 contracts should execute in < 100ms (actual: %dms)", duration)
                .isLessThan(100L);

        System.out.printf("✅ Sum of 100 contracts: %dms (expected < 100ms)%n", duration);
    }

    @Test
    @DisplayName("Sum endpoint should be performant with 1,000 contracts (< 200ms)")
    void sumShouldBePerformantWith1000Contracts() {
        for (int i = 1; i <= 1000; i++) {
            String contractPayload = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "500.00"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);

            if (i % 100 == 0) {
                System.out.printf("   Created %d/1000 contracts...%n", i);
            }
        }
        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get(ContractController.PATH_BASE + ContractController.PATH_SUM + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200);

        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
                .as("Sum of 1,000 contracts should execute in < 200ms (actual: %dms)", duration)
                .isLessThan(200L);

        System.out.printf("✅ Sum of 1,000 contracts: %dms (expected < 200ms)%n", duration);
    }

    @Test
    @DisplayName("Sum endpoint should only count ACTIVE contracts (performance + correctness)")
    void sumShouldOnlyCountActiveContractsPerformance() {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 100; i++) {
            String activeContract = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "1000.00"
                    }
                    """;

            given()
                    .contentType(ContentType.JSON)
                    .body(activeContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        for (int i = 1; i <= 100; i++) {
            String expiredContract = String.format("""
                    {
                        "startDate": "2024-01-01T00:00:00",
                        "endDate": "%s",
                        "costAmount": "999999.00"
                    }
                    """, now.minusDays(1));

            given()
                    .contentType(ContentType.JSON)
                    .body(expiredContract)
                    .post(ContractController.PATH_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }
        long startTime = System.currentTimeMillis();

        String sum = given()
                .when()
                .get(ContractController.PATH_BASE + ContractController.PATH_SUM + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .extract()
                .asString();

        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
                .as("Sum with active/expired mix should execute in < 100ms (actual: %dms)", duration)
                .isLessThan(100L);

        assertThat(sum).isEqualTo("100000.00");

        System.out.printf("✅ Sum of 100 active + 100 expired contracts: %dms (only active counted: %s)%n", duration, sum);
    }
}

