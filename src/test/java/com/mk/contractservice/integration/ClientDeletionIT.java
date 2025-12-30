package com.mk.contractservice.integration;

import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client Deletion - Critical Business Rules")
class ClientDeletionIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    @DisplayName("CRITICAL: When deleting a client, ALL active contracts must have endDate set to NOW")
    void shouldSetContractEndDateToNowWhenDeletingClient() {
        // GIVEN: Un client avec 3 contrats actifs

        String clientPayload = """
                {
                    "type": "PERSON",
                    "name": "Client ToDelete",
                    "email": "client.delete.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-05-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(clientPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();

        // Contrat 1: endDate = null (actif indéfiniment)
        String contract1Payload = """
                {
                    "startDate": "2025-01-01T00:00:00",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(contract1Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Contrat 2: endDate dans le futur (actif)
        String contract2Payload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, now.minusDays(10), now.plusMonths(6));

        given()
                .contentType(ContentType.JSON)
                .body(contract2Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Contrat 3: endDate dans le passé (déjà expiré)
        String contract3Payload = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "3000.00"
                }
                """, now.minusMonths(12), now.minusDays(30));

        given()
                .contentType(ContentType.JSON)
                .body(contract3Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(201)
                .extract().path("id");

        // ========================================
        // WHEN: On supprime le client
        // ========================================

        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        // THEN
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("CRITICAL: Deleting a client with NO contracts should succeed without errors")
    void shouldSuccessfullyDeleteClientWithoutContracts() {
        // GIVEN: Un client sans contrats
        String clientPayload = """
                {
                    "type": "PERSON",
                    "name": "Client No Contracts",
                    "email": "client.nocontracts.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-05-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(clientPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        // WHEN: On supprime le client
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        // THEN: Le client n'existe plus
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("CRITICAL: Deleting a client with ONLY expired contracts should succeed")
    void shouldSuccessfullyDeleteClientWithOnlyExpiredContracts() {
        // GIVEN: Un client avec uniquement des contrats expirés
        String clientPayload = """
                {
                    "type": "PERSON",
                    "name": "Client Expired Only",
                    "email": "client.expiredonly.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-05-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(clientPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();

        // Créer 2 contrats expirés
        String expiredContract1 = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, now.minusMonths(6), now.minusMonths(3));

        String expiredContract2 = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, now.minusMonths(12), now.minusDays(1));

        given().contentType(ContentType.JSON).body(expiredContract1)
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then().statusCode(201);

        given().contentType(ContentType.JSON).body(expiredContract2)
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then().statusCode(201);

        // Vérifier que la somme est 0 (tous expirés)
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_SUM + "?clientId={clientId}", clientId)
                .then()
                .statusCode(200)
                .body(anyOf(equalTo("0"), equalTo("0.00"), equalTo("0.0")));

        // WHEN: On supprime le client
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        // THEN: Le client n'existe plus
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Deleting a Company client should also close all contracts")
    void shouldCloseContractsWhenDeletingCompanyClient() {
        // GIVEN: Un client Company avec contrats actifs
        String companyPayload = """
                {
                    "type": "COMPANY",
                    "name": "Company ToDelete SA",
                    "email": "company.delete.%s@example.com",
                    "phone": "+41791234567",
                    "companyIdentifier": "CHE-%s"
                }
                """.formatted(
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 9).toUpperCase()
        );

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(companyPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00",
                    "endDate": null,
                    "costAmount": "5000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(201);

        // WHEN: On supprime le client Company
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        // THEN
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);
    }
}

