package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.features.contract.ImportContracts;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: A counselor imports contracts in bulk")
class ImportContractsAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private String clientId;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        clientId = createPerson();
    }

    private String createPerson() {
        return given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Marie Durand",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "1985-03-20"
                        }
                        """.formatted(TestDataHelper.uniqueEmail("marie.durand"),
                        TestDataHelper.randomSwissPhoneNumber()))
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String line(final String ownerId, final String costAmount) {
        return """
                { "clientId": "%s", "type": "LAMAL", "costAmount": "%s" }""".formatted(ownerId, costAmount);
    }

    private String importOf(final List<String> lines) {
        return """
                { "contracts": [%s] }""".formatted(String.join(",", lines));
    }

    private String importOf(final String... lines) {
        return importOf(List.of(lines));
    }

    private String isoDateTime(final LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private ValidatableResponse contractsOfClient() {
        return given().when().get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId).then();
    }

    private void assertClientHasNoContract() {
        contractsOfClient().statusCode(200).body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("GIVEN valid lines for several clients WHEN importing THEN every contract exists and the ids follow the lines order")
    void shouldImportContractsForSeveralClients() {
        final String otherClientId = createPerson();
        final String start = isoDateTime(LocalDateTime.now(clock).plusDays(1).truncatedTo(ChronoUnit.SECONDS));
        final String end = isoDateTime(LocalDateTime.now(clock).plusYears(1).truncatedTo(ChronoUnit.SECONDS));

        final List<String> contractIds = given()
                .body(importOf(
                        """
                                { "clientId": "%s", "type": "LAMAL", "startDate": "%s", "endDate": "%s", "costAmount": "1000.00" }"""
                                .formatted(clientId, start, end),
                        line(otherClientId, "2500.50")
                ))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(201)
                .body("importedCount", equalTo(2))
                .body("contractIds", hasSize(2))
                .extract()
                .path("contractIds");

        given().when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractIds.getFirst())
                .then()
                .statusCode(200)
                .body("clientId", equalTo(clientId))
                .body("costAmount", equalTo(1000.00f))
                .body("startDate", equalTo(start))
                .body("endDate", equalTo(end));

        given().when()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractIds.getLast())
                .then()
                .statusCode(200)
                .body("clientId", equalTo(otherClientId))
                .body("costAmount", equalTo(2500.50f));
    }

    @Test
    @DisplayName("GIVEN an imported contract WHEN looking at the client THEN it counts in its active contracts and total")
    void shouldCountImportedContractsInClientTotal() {
        given()
                .body(importOf(line(clientId, "1000.00"), line(clientId, "500.00")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(201);

        contractsOfClient().statusCode(200).body("totalElements", equalTo(2));

        given().when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}",
                        clientId)
                .then()
                .statusCode(200)
                .body(equalTo("1500.00"));
    }

    @Test
    @DisplayName("GIVEN one invalid line among valid ones WHEN importing THEN nothing is imported and the line is reported")
    void shouldImportNothingWhenOneLineIsInvalid() {
        given()
                .body(importOf(line(clientId, "1000.00"), line(clientId, "-5.00"), line(clientId, "2000.00")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("contractImportRejected"))
                .body("rejections", hasSize(1))
                .body("rejections[0].index", equalTo(1))
                .body("rejections[0].code", equalTo("invalidContractCost"));

        assertClientHasNoContract();
    }

    @Test
    @DisplayName("GIVEN an employer insurance (LPP) for a person WHEN importing THEN nothing is imported and the line is reported")
    void shouldImportNothingWhenATypeIsNotAllowedForTheClient() {
        given()
                .body(importOf(line(clientId, "1000.00"),
                        """
                                { "clientId": "%s", "type": "LPP", "costAmount": "500.00" }""".formatted(clientId)))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("contractImportRejected"))
                .body("rejections", hasSize(1))
                .body("rejections[0].index", equalTo(1))
                .body("rejections[0].code", equalTo("contractTypeNotAllowed"));

        assertClientHasNoContract();
    }

    @Test
    @DisplayName("GIVEN several problems WHEN importing THEN all of them are reported in one answer")
    void shouldReportEveryProblemAtOnce() {
        final String unknownClientId = UUID.randomUUID().toString();
        final LocalDateTime start = LocalDateTime.now(clock);

        given()
                .body(importOf(
                        line(clientId, "0.00"),
                        line(clientId, "1000.00"),
                        """
                                { "clientId": "%s", "type": "LAMAL", "startDate": "%s", "endDate": "%s", "costAmount": "1000.00" }"""
                                .formatted(clientId, start, start.minusDays(1)),
                        line(unknownClientId, "1000.00")
                ))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("rejections", hasSize(3))
                .body("rejections[0].index", equalTo(0))
                .body("rejections[0].clientId", equalTo(clientId))
                .body("rejections[0].code", equalTo("invalidContractCost"))
                .body("rejections[1].index", equalTo(2))
                .body("rejections[1].clientId", equalTo(clientId))
                .body("rejections[1].code", equalTo("invalidContractPeriod"))
                .body("rejections[2].index", equalTo(3))
                .body("rejections[2].clientId", equalTo(unknownClientId))
                .body("rejections[2].code", equalTo("clientNotFound"))
                .body("rejections[2].message", containsString(unknownClientId));

        assertClientHasNoContract();
    }

    @Test
    @DisplayName("GIVEN a deleted client WHEN importing a contract for it THEN the line is rejected as an unknown client")
    void shouldRejectLinesForDeletedClient() {
        given().when().delete(ClientEndpoints.CLIENT_BY_ID, clientId).then().statusCode(204);

        given()
                .body(importOf(line(clientId, "1000.00")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("rejections[0].index", equalTo(0))
                .body("rejections[0].code", equalTo("clientNotFound"));
    }

    @Test
    @DisplayName("GIVEN the maximum number of lines WHEN importing THEN every contract is imported")
    void shouldImportMaximumNumberOfLines() {
        given()
                .body(importOf(Collections.nCopies(ImportContracts.MAX_LINES, line(clientId, "10.00"))))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(201)
                .body("importedCount", equalTo(ImportContracts.MAX_LINES));

        contractsOfClient().statusCode(206).body("totalElements", equalTo(ImportContracts.MAX_LINES));
    }

    @Test
    @DisplayName("GIVEN one line too many WHEN importing THEN the import is refused as a whole")
    void shouldRefuseImportAboveMaximum() {
        given()
                .body(importOf(Collections.nCopies(ImportContracts.MAX_LINES + 1, line(clientId, "10.00"))))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("invalidImportSize"));

        assertClientHasNoContract();
    }

    @Test
    @DisplayName("GIVEN an empty import WHEN importing THEN it is refused")
    void shouldRefuseEmptyImport() {
        given()
                .body(importOf())
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("invalidImportSize"));
    }

    @Test
    @DisplayName("GIVEN a line without cost WHEN importing THEN the error names the line and the field")
    void shouldNameLineAndFieldOfMissingValue() {
        given()
                .body(importOf(line(clientId, "1000.00"), """
                        { "clientId": "%s", "type": "LAMAL" }""".formatted(clientId)))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations[0].field", equalTo("contracts[1].costAmount"));
    }

    @Test
    @DisplayName("GIVEN an empty line WHEN importing THEN the error names the line")
    void shouldNameEmptyLine() {
        given()
                .body(importOf(line(clientId, "1000.00"), "null"))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations[0].field", equalTo("contracts[1]"));
    }

    @Test
    @DisplayName("GIVEN a value of the wrong type WHEN importing THEN the error names the line and the field")
    void shouldNameLineAndFieldOfWrongType() {
        given()
                .body(importOf(line(clientId, "1000.00"), line(clientId, "not-a-number")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("detail", containsString("contracts[1].costAmount"));
    }

    @Test
    @DisplayName("GIVEN no contracts field WHEN importing THEN it is refused")
    void shouldRefuseMissingContractsField() {
        given()
                .body("{}")
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations.field", hasItem("contracts"));
    }

    @Test
    @DisplayName("GIVEN a rejected import fixed by the counselor WHEN importing again THEN each contract exists only once")
    void shouldNotDuplicateContractsWhenRetryingRejectedImport() {
        given()
                .body(importOf(line(clientId, "1000.00"), line(clientId, "-1.00")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422);

        given()
                .body(importOf(line(clientId, "1000.00"), line(clientId, "1.00")))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(201);

        contractsOfClient().statusCode(200).body("content.costAmount", containsInAnyOrder(1000.00f, 1.00f));
    }
}
