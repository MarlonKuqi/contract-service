package com.mk.contractservice.integration;

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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Optimistic Locking - Integration Tests")
class OptimisticLockingIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Sequential updates should succeed - version increments transparently")
    void shouldAllowSequentialUpdates() {
        String uniqueEmail = "sequential." + UUID.randomUUID() + "@example.com";

        String clientId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Alice Martin",
                            "email": "%s",
                            "phone": "+41791234567",
                            "birthDate": "1990-05-15"
                        }
                        """.formatted(uniqueEmail))
                .post("/v2/clients")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alice Martin Updated 1",
                            "email": "%s",
                            "phone": "+41791234567"
                        }
                        """.formatted(uniqueEmail))
                .put("/v2/clients/{id}", clientId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alice Martin Updated 2",
                            "email": "%s",
                            "phone": "+41791234567"
                        }
                        """.formatted(uniqueEmail))
                .put("/v2/clients/{id}", clientId)
                .then()
                .statusCode(204);

        given()
                .get("/v2/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Alice Martin Updated 2"));
    }

    @Test
    @DisplayName("Sequential contract cost updates should succeed")
    void shouldAllowSequentialContractCostUpdates() {
        String uniqueEmail = "contract." + UUID.randomUUID() + "@example.com";

        String clientId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Contract Owner",
                            "email": "%s",
                            "phone": "+41791234567",
                            "birthDate": "1990-05-15"
                        }
                        """.formatted(uniqueEmail))
                .post("/v2/clients")
                .then()
                .statusCode(201)
                .extract().path("id");

        String contractId = given()
                .contentType(ContentType.JSON)
                .queryParam("clientId", clientId)
                .body("""
                        {
                            "startDate": "2025-01-01T00:00:00",
                            "endDate": "2026-01-01T00:00:00",
                            "costAmount": "1000.00"
                        }
                        """)
                .when()
                .post("/v2/contracts")
                .then()
                .statusCode(201)
                .extract().path("id");

        for (int i = 1; i <= 5; i++) {
            String updatePayload = String.format("""
                    {
                        "amount": "%d00.00"
                    }
                    """, 10 + i);

            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayload)
                    .queryParam("clientId", clientId)
                    .patch("/v2/contracts/{contractId}/cost", contractId)
                    .then()
                    .statusCode(204);
        }

        given()
                .queryParam("clientId", clientId)
                .get("/v2/contracts/{contractId}", contractId)
                .then()
                .statusCode(200)
                .body("costAmount", equalTo(1500.00f));
    }
}

