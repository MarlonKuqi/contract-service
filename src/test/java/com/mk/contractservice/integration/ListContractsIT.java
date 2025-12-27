package com.mk.contractservice.integration;

import com.mk.contractservice.application.feature.contract.shared.constants.ContractEndpoints;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("List Contracts - Integration Tests")
class ListContractsIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private ContractService contractService;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        testClient = Person.of(
                ClientName.of("John Doe"),
                ClientEmail.of("john.pagination." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@test.com"),
                ClientPhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        testClient = clientRepository.save(testClient);
    }

    @Nested
    @DisplayName("Pagination Functionality")
    class PaginationFunctionalityTests {

        @Test
        @DisplayName("Should navigate across multiple pages with default page size")
        void shouldPaginateContractsAcrossMultiplePages() {
            final int pageSize = 10;
            createContracts(25, 100);
            givenPaginationParams(0, pageSize)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .header("Content-Range", org.hamcrest.Matchers.containsString("contracts 0-9/25"))
                    .body("content.size()", equalTo(10))
                    .body("pageNumber", equalTo(0))
                    .body("pageSize", equalTo(pageSize))
                    .body("totalElements", equalTo(25))
                    .body("totalPages", equalTo(3))
                    .body("isFirst", equalTo(true))
                    .body("isLast", equalTo(false));
            givenPaginationParams(1, pageSize)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .header("Content-Range", org.hamcrest.Matchers.containsString("contracts 10-19/25"))
                    .body("content.size()", equalTo(10))
                    .body("pageNumber", equalTo(1))
                    .body("pageSize", equalTo(10))
                    .body("totalElements", equalTo(25))
                    .body("totalPages", equalTo(3))
                    .body("isFirst", equalTo(false))
                    .body("isLast", equalTo(false));
            givenPaginationParams(2, pageSize)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .header("Content-Range", org.hamcrest.Matchers.containsString("contracts 20-24/25"))
                    .body("content.size()", equalTo(5))
                    .body("pageNumber", equalTo(2))
                    .body("pageSize", equalTo(pageSize))
                    .body("totalElements", equalTo(25))
                    .body("totalPages", equalTo(3))
                    .body("isFirst", equalTo(false))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should return empty page when requesting page beyond available data")
        void shouldReturnEmptyPageWhenBeyondAvailableData() {
            createContracts(5, 100);

            givenPaginationParams(5, 10)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .header("Content-Range", org.hamcrest.Matchers.matchesRegex("contracts .*/5"))
                    .body("content.size()", equalTo(0))
                    .body("pageNumber", equalTo(5))
                    .body("pageSize", equalTo(10))
                    .body("totalElements", equalTo(5))
                    .body("totalPages", equalTo(1))
                    .body("isFirst", equalTo(false))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should sort contracts by lastModified descending")
        void shouldSortContractsByLastModifiedDescending() {
            String oldContract = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "100.00"
                    }
                    """;

            String middleContract = """
                    {
                        "startDate": "2025-01-15T00:00:00",
                        "endDate": null,
                        "costAmount": "200.00"
                    }
                    """;

            String recentContract = """
                    {
                        "startDate": "2025-02-01T00:00:00",
                        "endDate": null,
                        "costAmount": "300.00"
                    }
                    """;

            // Create contracts with explicit delays to ensure distinct lastModified timestamps
            given().contentType(ContentType.JSON).body(middleContract)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2)).until(() -> true);

            given().contentType(ContentType.JSON).body(oldContract)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2)).until(() -> true);

            given().contentType(ContentType.JSON).body(recentContract)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then().statusCode(201);

            // Wait a bit to ensure all transactions are committed
            await().pollDelay(Duration.ofMillis(50)).atMost(Duration.ofSeconds(1)).until(() -> true);

            given()
                    .queryParam("sort", "lastModified,desc")
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(3))
                    .body("content[0].costAmount", equalTo(300.00f))  // most recent
                    .body("content[1].costAmount", equalTo(100.00f))  // middle
                    .body("content[2].costAmount", equalTo(200.00f)); // oldest
        }

        @Test
        @DisplayName("Should combine pagination with updatedSince filter")
        void shouldFilterAndPaginateByUpdatedSince() {
            // GIVEN: Create 5 contracts BEFORE the filter date
            for (int i = 1; i <= 5; i++) {
                String oldContract = String.format("""
                        {
                            "startDate": "2025-01-01T00:00:00",
                            "endDate": null,
                            "costAmount": "%d.00"
                        }
                        """, i * 100);

                given().contentType(ContentType.JSON).body(oldContract)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then().statusCode(201);
            }

            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2)).until(() -> true);

            LocalDateTime filterDate = LocalDateTime.now();

            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2)).until(() -> true);

            for (int i = 6; i <= 10; i++) {
                String recentContract = String.format("""
                        {
                            "startDate": "2025-02-01T00:00:00",
                            "endDate": null,
                            "costAmount": "%d.00"
                        }
                        """, i * 100);

                given().contentType(ContentType.JSON).body(recentContract)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then().statusCode(201);
            }

            given()
                    .queryParam("updatedSince", filterDate.toString())
                    .queryParam("page", 0)
                    .queryParam("size", 5)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(5))
                    .body("totalElements", equalTo(5))
                    .body("totalPages", equalTo(1));
        }

        @Test
        @DisplayName("Should use default page size when not specified")
        void shouldUseDefaultPageSizeWhenNotSpecified() {
            createContracts(25, 100);

            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .body("content.size()", equalTo(20))
                    .body("pageSize", equalTo(20))
                    .body("totalElements", equalTo(25));
        }

        @Test
        @DisplayName("Should handle different page sizes correctly")
        void shouldHandleDifferentPageSizes() {
            createContracts(50, 100);

            given()
                    .queryParam("size", 5)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .body("content.size()", equalTo(5))
                    .body("pageSize", equalTo(5))
                    .body("totalPages", equalTo(10));

            given()
                    .queryParam("size", 25)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .body("content.size()", equalTo(25))
                    .body("pageSize", equalTo(25))
                    .body("totalPages", equalTo(2));

            given()
                    .queryParam("size", 50)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(50))
                    .body("pageSize", equalTo(50))
                    .body("totalPages", equalTo(1))
                    .body("isFirst", equalTo(true))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should return correct metadata for single page result")
        void shouldReturnCorrectMetadataForSinglePage() {
            createContracts(3, 100);

            given()
                    .queryParam("size", 20)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(3))
                    .body("pageNumber", equalTo(0))
                    .body("pageSize", equalTo(20))
                    .body("totalElements", equalTo(3))
                    .body("totalPages", equalTo(1))
                    .body("isFirst", equalTo(true))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should return empty page when client has no contracts")
        void shouldReturnEmptyPageWhenNoContracts() {

            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(0))
                    .body("pageNumber", equalTo(0))
                    .body("totalElements", equalTo(0))
                    .body("totalPages", equalTo(0))
                    .body("isFirst", equalTo(true))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should only return active contracts in pagination")
        void shouldOnlyReturnActiveContractsInPagination() {
            for (int i = 1; i <= 5; i++) {
                String expiredContract = String.format("""
                        {
                            "startDate": "2024-01-01T00:00:00",
                            "endDate": "2024-12-31T23:59:59",
                            "costAmount": "%d.00"
                        }
                        """, i * 100);

                given()
                        .contentType(ContentType.JSON)
                        .body(expiredContract)
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
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
                        .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                        .then()
                        .statusCode(201);
            }

            given()
                    .queryParam("size", 5)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(206)
                    .body("content.size()", equalTo(5))
                    .body("totalElements", equalTo(10))  // Only active contracts
                    .body("totalPages", equalTo(2));
        }

        @Test
        @DisplayName("Should return 200 OK when all results fit in one page (single contract, no pagination)")
        void shouldReturn200WhenNoPaginationNeeded() {
            createContract(2500);
            given()
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(1))
                    .body("totalElements", equalTo(1))
                    .body("totalPages", equalTo(1))
                    .body("isFirst", equalTo(true))
                    .body("isLast", equalTo(true));
        }

        @Test
        @DisplayName("Should enforce maximum page size limit (100)")
        void shouldEnforceMaxPageSizeLimit() {
            createContracts(50, 100);

            given()
                    .queryParam("size", 200)
                    .when()
                    .get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(400)
                    .body("title", equalTo("Invalid Parameter"))
                    .body("detail", org.hamcrest.Matchers.containsString("Page size must not exceed 100"));
        }

        private void createContract(double costAmount) {
            String contractPayload = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": null,
                        "costAmount": "%s"
                    }
                    """.formatted(String.format(java.util.Locale.US, "%.2f", costAmount));

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", testClient.getId())
                    .then()
                    .statusCode(201);
        }

        private void createContracts(int count, int amount) {
            for (int i = 1; i <= count; i++) {
                createContract(i * amount);
            }
        }

        private RequestSpecification givenPaginationParams(int pageNumber, int pageSize) {
            return given()
                    .queryParam("page", pageNumber)
                    .queryParam("size", pageSize);
        }
    }

    @Nested
    @DisplayName("Pagination Validation")
    class PaginationValidationTests {

        @Test
        @DisplayName("Should accept valid pagination parameters")
        void shouldAcceptValidPagination() {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", 0)
                    .param("size", 20)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }

        @ParameterizedTest
        @CsvSource({
                "-1, 20, 'Page number must not be less than zero'",
                "-10, 20, 'Page number must not be less than zero'",
                "-100, 20, 'Page number must not be less than zero'"
        })
        @DisplayName("Should reject negative page numbers")
        void shouldRejectNegativePageNumber(int page, int size, String expectedMessage) {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", page)
                    .param("size", size)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .log().all()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("title", containsString("Invalid Parameter"))
                    .body("detail", containsString(expectedMessage));
        }

        @ParameterizedTest
        @CsvSource({
                "0, 0, 'Page size must not be less than one'",
                "0, -1, 'Page size must not be less than one'",
                "0, -10, 'Page size must not be less than one'"
        })
        @DisplayName("Should reject page size less than 1")
        void shouldRejectPageSizeLessThanOne(int page, int size, String expectedMessage) {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", page)
                    .param("size", size)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("title", containsString("Invalid Parameter"))
                    .body("detail", containsString(expectedMessage));
        }

        @ParameterizedTest
        @CsvSource({
                "0, 101, 'Page size must not exceed 100'",
                "0, 150, 'Page size must not exceed 100'",
                "0, 200, 'Page size must not exceed 100'"
        })
        @DisplayName("Should reject page size greater than max")
        void shouldRejectPageSizeGreaterThanMax(int page, int size, String expectedMessage) {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", page)
                    .param("size", size)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("title", containsString("Invalid Parameter"))
                    .body("detail", containsString(expectedMessage));
        }

        @Test
        @DisplayName("Should accept page size equal to max (100)")
        void shouldAcceptPageSizeEqualToMax() {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", 0)
                    .param("size", 100)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }

        @ParameterizedTest
        @CsvSource({
                "abc, 20, 'Invalid page number format'",
                "0, xyz, 'Invalid page size format'"
        })
        @DisplayName("Should reject invalid number formats")
        void shouldRejectInvalidFormats(String page, String size, String expectedMessage) {
            UUID clientId = UUID.randomUUID();

            given()
                    .param("clientId", clientId.toString())
                    .param("page", page)
                    .param("size", size)
                    .when()
                    .get("/v2/contracts")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("title", containsString("Invalid Parameter"))
                    .body("detail", containsString(expectedMessage));
        }
    }
}

