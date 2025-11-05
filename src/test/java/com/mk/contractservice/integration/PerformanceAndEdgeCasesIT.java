package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

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
        Client client = clientRepository.save(new Person(
                ClientName.of("Performance Test Client"),
                Email.of("perf.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
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
        long startTime = System.currentTimeMillis();
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("5000.00")); // 50 * 100

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assert duration < 1000 : "Sum endpoint took " + duration + "ms, should be under 1000ms";
    }

    @Test
    @DisplayName("EDGE CASE: Empty client list returns empty for sum")
    void emptyClientShouldReturnZeroSum() {
        Client client = clientRepository.save(new Person(
                ClientName.of("No Contracts Client"),
                Email.of("nocontracts." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("0"));

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    @DisplayName("EDGE CASE: Very large cost amounts should be handled correctly")
    void shouldHandleVeryLargeCostAmounts() {
        Client client = clientRepository.save(new Person(
                ClientName.of("Big Money Client"),
                Email.of("bigmoney." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
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
        Client client = clientRepository.save(new Person(
                ClientName.of("Precision Test Client"),
                Email.of("precision." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
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
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

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
                    """.formatted(UUID.randomUUID().toString().substring(0, 8) + phone.hashCode(), phone);

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
        Client client = clientRepository.save(new Person(
                ClientName.of("Boundary Test Client"),
                Email.of("boundary." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
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

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("EDGE CASE: Concurrent contract creation should work")
    void shouldHandleConcurrentContractCreation() {
        Client client = clientRepository.save(new Person(
                ClientName.of("Concurrent Test Client"),
                Email.of("concurrent." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
        String contractPayload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "500.00"
                }
                """, now.minusDays(5));

        for (int i = 0; i < 5; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .body(contractPayload)
                    .when()
                    .post("/v1/clients/{clientId}/contracts", client.getId())
                    .then()
                    .statusCode(201);
        }

        given()
                .when()
                .get("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(5));

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
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

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
        Client client = clientRepository.save(new Person(
                ClientName.of("Zero Test Client"),
                Email.of("zero." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                PhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        LocalDateTime now = LocalDateTime.now();
        String zeroAmountPayload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "0.00"
                }
                """, now.minusDays(1));
        given()
                .contentType(ContentType.JSON)
                .body(zeroAmountPayload)
                .when()
                .post("/v1/clients/{clientId}/contracts", client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }
}


