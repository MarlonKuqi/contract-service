package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client Deletion - Critical Business Rules")
class ClientDeletionAcceptanceTest {

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
    @DisplayName("CRITICAL: When deleting a client, ALL active contracts must have endDate set to NOW")
    void shouldSetContractEndDateToNowWhenDeletingClient() {
        // GIVEN: Un client avec 3 contrats actifs

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String phoneNumber = "+41791234567";

        String clientPayload = """
                {
                    "type": "PERSON",
                    "name": "Client ToDelete",
                    "email": "client.delete.%s@example.com",
                    "phone": "%s",
                    "birthDate": "1990-05-15"
                }
                """.formatted(uniqueId, phoneNumber);

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(clientPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();

        // Contrat 1: endDate = null (actif indefiniment)
        String contract1Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": null,
                    "costAmount": "1000.00"
                }
                """, clientId);

        String contract1Id = given()
                .contentType(ContentType.JSON)
                .body(contract1Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Contrat 2: endDate dans le futur (actif)
        String contract2Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, clientId, now.minusDays(10), now.plusMonths(6));

        String contract2Id = given()
                .contentType(ContentType.JSON)
                .body(contract2Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        // Contrat 3: endDate dans le passe (dejà expire)
        String contract3Payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "3000.00"
                }
                """, clientId, now.minusMonths(12), now.minusDays(30));

        String contract3Id = given()
                .contentType(ContentType.JSON)
                .body(contract3Payload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
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

        LocalDateTime deletionTime = LocalDateTime.now();

        // THEN: Le client est supprime
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);

        // ========================================
        // THEN: Verification que les contrats ACTIFS ont ete fermes avec endDate = deletionTime
        // ========================================

        // Contrat 1 (etait actif indefiniment) => doit etre ferme avec endDate = deletionTime
        String endDate1Str = given()
                .queryParam("clientId", clientId)
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract1Id)
                .then()
                .statusCode(200)
                .body("id", equalTo(contract1Id))
                .extract().path("endDate");

        assertThat(endDate1Str).isNotNull();
        LocalDateTime endDate1 = LocalDateTime.parse(endDate1Str);
        assertThat(endDate1).isCloseTo(deletionTime, within(10, ChronoUnit.SECONDS));

        // Contrat 2 (etait actif avec endDate future) => doit etre ferme avec endDate = deletionTime
        String endDate2Str = given()
                .queryParam("clientId", clientId)
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract2Id)
                .then()
                .statusCode(200)
                .body("id", equalTo(contract2Id))
                .extract().path("endDate");

        assertThat(endDate2Str).isNotNull();
        LocalDateTime endDate2 = LocalDateTime.parse(endDate2Str);
        assertThat(endDate2).isCloseTo(deletionTime, within(10, ChronoUnit.SECONDS));

        // Contrat 3 (etait dejà expire) => endDate ne doit PAS avoir change (reste celle d'origine)
        String endDate3Str = given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract3Id)
                .then()
                .statusCode(200)
                .body("id", equalTo(contract3Id))
                .extract().path("endDate");

        assertThat(endDate3Str).isNotNull();
        LocalDateTime endDate3 = LocalDateTime.parse(endDate3Str);
        assertThat(endDate3).isCloseTo(now.minusDays(30), within(1, ChronoUnit.SECONDS));
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
        // GIVEN: Un client avec uniquement des contrats expires
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String clientPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "Client Expired Only",
                    "email": "client.expiredonly.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-05-15"
                }
                """, uniqueId);

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(clientPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredEndDate1 = now.minusMonths(3);
        LocalDateTime expiredEndDate2 = now.minusDays(1);

        // Creer 2 contrats expires
        String expiredContract1 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "1000.00"
                }
                """, clientId, now.minusMonths(6), expiredEndDate1);

        String expiredContract2 = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": "%s",
                    "costAmount": "2000.00"
                }
                """, clientId, now.minusMonths(12), expiredEndDate2);

        String contract1Id = given().contentType(ContentType.JSON).body(expiredContract1)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201)
                .extract().path("id");

        String contract2Id = given().contentType(ContentType.JSON).body(expiredContract2)
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201)
                .extract().path("id");

        // WHEN: On supprime le client
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        LocalDateTime deletionTime = LocalDateTime.now();

        // THEN: Le client n'existe plus
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);

        // THEN: Les contrats expires ne doivent PAS avoir change d'endDate (aucune modification)
        String endDate1Str = given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract1Id)
                .then()
                .statusCode(200)
                .body("id", equalTo(contract1Id))
                .extract().path("endDate");

        assertThat(endDate1Str).isNotNull();
        LocalDateTime endDate1 = LocalDateTime.parse(endDate1Str);
        assertThat(endDate1).isCloseTo(expiredEndDate1, within(1, ChronoUnit.SECONDS));
        assertThat(endDate1).isBefore(deletionTime.minusMonths(2));

        String endDate2Str = given()
                .queryParam("clientId", clientId)
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract2Id)
                .then()
                .statusCode(200)
                .body("id", equalTo(contract2Id))
                .extract().path("endDate");

        assertThat(endDate2Str).isNotNull();
        LocalDateTime endDate2 = LocalDateTime.parse(endDate2Str);
        assertThat(endDate2).isCloseTo(expiredEndDate2, within(1, ChronoUnit.SECONDS));
        assertThat(endDate2).isBefore(deletionTime.minusHours(1));
    }

    @Test
    @DisplayName("SCENARIO: Deleting a Company client should also close all contracts")
    void shouldCloseContractsWhenDeletingCompanyClient() {
        // GIVEN: Un client Company avec contrats actifs
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        String companyPayload = """
                {
                    "type": "COMPANY",
                    "name": "Company ToDelete SA",
                    "email": "company.delete.%s@example.com",
                    "phone": "+41791111004",
                    "companyIdentifier": "CHE-%s"
                }
                """.formatted(
                uniqueId,
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

        // Contrat actif indefiniment
        String contractPayload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": null,
                    "costAmount": "5000.00"
                }
                """, clientId, LocalDateTime.now().minusDays(30));

        String contractId = given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        // WHEN: On supprime le client Company
        given()
                .when()
                .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(204);

        LocalDateTime deletionTime = LocalDateTime.now();

        // THEN: Le client n'existe plus
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(404);

        // THEN: Le contrat actif est ferme (endDate = now)
        String endDateStr = given()
                .when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contractId))
                .extract().path("endDate");

        assertThat(endDateStr).isNotNull();
        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
        assertThat(endDate).isCloseTo(deletionTime, within(10, ChronoUnit.SECONDS));
    }
}

