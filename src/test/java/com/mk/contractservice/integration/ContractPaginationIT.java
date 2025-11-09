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

import static io.restassured.RestAssured.given;
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("page", 5)
                .queryParam("size", 10)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then().statusCode(201);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given().contentType(ContentType.JSON).body(oldContract)
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then().statusCode(201);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given().contentType(ContentType.JSON).body(recentContract)
                .post("/v1/clients/{clientId}/contracts", testClient.getId())
                .then().statusCode(201);

        given()
                .queryParam("sort", "lastModified,desc")
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                // Most recent first (created last)
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 5)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("pageSize", equalTo(5))
                .body("totalPages", equalTo(10));

        given()
                .queryParam("size", 25)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(25))
                .body("pageSize", equalTo(25))
                .body("totalPages", equalTo(2));

        given()
                .queryParam("size", 50)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 20)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .queryParam("size", 5)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5))
                .body("totalElements", equalTo(10))
                .body("totalPages", equalTo(2));
    }

    @Test
    @DisplayName("Should enforce max page size limit when requested size exceeds maximum")
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
                    .post("/v1/clients/{clientId}/contracts", testClient.getId())
                    .then()
                    .statusCode(201);
        }
        given()
                .queryParam("size", 200)
                .when()
                .get("/v1/clients/{clientId}/contracts", testClient.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(50))
                .body("pageSize", equalTo(100));
    }
}

