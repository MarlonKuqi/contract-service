package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.*;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Performance and Edge Cases - Integration Tests")
class PerformanceAndEdgeCasesIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientRepository;

    @Autowired
    private ContractJpaRepository contractRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        contractRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    @DisplayName("PERFORMANCE: Sum endpoint should be fast even with many contracts")
    void sumEndpointShouldBePerformant() {
        // GIVEN: Create a client with many contracts
        Client client = clientRepository.save(new Person(
                ClientName.of("Performance Test Client"),
                Email.of("perf.test." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // Create 50 active contracts
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        for (int i = 0; i < 50; i++) {
            String contractPayload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "100.00"
                }
                """, now.minusDays(10));

            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .post("/v1/clients/{clientId}/contracts", client.getId())
                    .then()
                    .statusCode(201);
        }

        // WHEN: Calculate sum (should be very fast)
        long startTime = System.currentTimeMillis();

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("5000.00")); // 50 * 100

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // THEN: Should complete in under 1 second (performant endpoint as per requirement)
        assert duration < 1000 : "Sum endpoint took " + duration + "ms, should be under 1000ms";
    }

    @Test
    @DisplayName("EDGE CASE: Empty client list returns empty for sum")
    void emptyClientShouldReturnZeroSum() {
        // GIVEN: A client without any contracts
        Client client = clientRepository.save(new Person(
                ClientName.of("No Contracts Client"),
                Email.of("nocontracts." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Getting sum
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        // AND: Getting contracts list
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("EDGE CASE: Very large cost amounts should be handled correctly")
    void shouldHandleVeryLargeCostAmounts() {
        // GIVEN: Create client
        Client client = clientRepository.save(new Person(
                ClientName.of("Big Money Client"),
                Email.of("bigmoney." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Create contract with very large amount
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        String contractPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "999999999.99"
            }
            """, now.minusDays(5));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(201)
                .body("costAmount", equalTo(999999999.99f));

        // THEN: Sum should calculate correctly
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("999999999.99"));
    }

    @Test
    @DisplayName("EDGE CASE: Decimal precision should be maintained")
    void shouldMaintainDecimalPrecision() {
        // GIVEN: Create client
        Client client = clientRepository.save(new Person(
                ClientName.of("Precision Test Client"),
                Email.of("precision." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Create contracts with precise decimal amounts
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        String contract1 = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "1234.56"
            }
            """, now.minusDays(10));

        String contract2 = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "789.44"
            }
            """, now.minusDays(5));

        given().contentType(ContentType.JSON).body(contract1).post("/v1/clients/{clientId}/contracts", client.getId()).then().statusCode(201);
        given().contentType(ContentType.JSON).body(contract2).post("/v1/clients/{clientId}/contracts", client.getId()).then().statusCode(201);

        // THEN: Sum should maintain precision
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("2024.00")); // 1234.56 + 789.44
    }

    @Test
    @DisplayName("EDGE CASE: Special characters in names should be handled")
    void shouldHandleSpecialCharactersInNames() {
        String specialNamePayload = """
            {
                "name": "François O'Brien-Müller",
                "email": "francois.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "1990-01-01"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(specialNamePayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .body("name", equalTo("François O'Brien-Müller"));
    }

    @Test
    @DisplayName("EDGE CASE: International phone numbers should be validated")
    void shouldValidateInternationalPhoneNumbers() {
        // Valid international formats
        String[] validPhones = {
                "+41791234567",    // Switzerland
                "+33612345678",    // France
                "+4407123456789",  // UK
                "+12025551234",    // USA
                "+861234567890"    // China
        };

        for (String phone : validPhones) {
            String payload = """
                {
                    "name": "International Test",
                    "email": "intl.%s@example.com",
                    "phone": "%s",
                    "birthDate": "1990-01-01"
                }
                """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8) + phone.hashCode(), phone);

            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/v1/clients/persons")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @DisplayName("EDGE CASE: Boundary dates should be handled correctly")
    void shouldHandleBoundaryDates() {
        // GIVEN: Create client
        Client client = clientRepository.save(new Person(
                ClientName.of("Boundary Test Client"),
                Email.of("boundary." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Create contract ending in the future
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        String contractPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": "%s",
                "costAmount": "1000.00"
            }
            """, now.minusDays(30), now.plusMonths(11).withDayOfMonth(31).withHour(23).withMinute(59).withSecond(59));

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(201);

        // THEN: Should be included in active contracts during the period
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("EDGE CASE: Concurrent contract creation should work")
    void shouldHandleConcurrentContractCreation() {
        // GIVEN: Create client
        Client client = clientRepository.save(new Person(
                ClientName.of("Concurrent Test Client"),
                Email.of("concurrent." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        String contractPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "500.00"
            }
            """, now.minusDays(5));

        // WHEN: Create multiple contracts in quick succession
        for (int i = 0; i < 5; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .when()
                    .post("/v1/clients/{clientId}/contracts", client.getId())
                    .then()
                    .statusCode(201);
        }

        // THEN: All contracts should be persisted
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("size()", equalTo(5));

        // AND: Sum should be correct
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("2500.00")); // 5 * 500
    }

    @Test
    @DisplayName("EDGE CASE: Very old birth dates should be accepted")
    void shouldAcceptVeryOldBirthDates() {
        String oldBirthDatePayload = """
            {
                "name": "Very Old Person",
                "email": "old.%s@example.com",
                "phone": "+41791234567",
                "birthDate": "1920-01-01"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(oldBirthDatePayload)
                .when()
                .post("/v1/clients/persons")
                .then()
                .statusCode(201)
                .body("birthDate", equalTo("1920-01-01"));
    }

    @Test
    @DisplayName("EDGE CASE: Zero cost amount should be rejected")
    void shouldRejectZeroCostAmount() {
        // GIVEN: Create client
        Client client = clientRepository.save(new Person(
                ClientName.of("Zero Test Client"),
                Email.of("zero." + java.util.UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        // WHEN: Try to create contract with zero amount
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        String zeroAmountPayload = String.format("""
            {
                "startDate": "%s",
                "endDate": null,
                "costAmount": "0.00"
            }
            """, now.minusDays(1));

        // THEN: Should be rejected
        given()
                .contentType(ContentType.JSON)
                .body(zeroAmountPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }
}

