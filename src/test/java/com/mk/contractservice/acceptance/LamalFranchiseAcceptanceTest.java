package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.ETags;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("LAMal Franchise - Acceptance Tests")
class LamalFranchiseAcceptanceTest {

    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private LocalDateTime today;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        today = LocalDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("SCENARIO: An adult takes out a LAMal with a 2500 CHF franchise, returned on creation and on read")
    void shouldCreateLamalWithFranchise() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        String contractId = createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 2500, "costAmount": "250.00" }
                """.formatted(adult.id()))
                .statusCode(201)
                .body("franchise", equalTo(2500))
                .extract()
                .path("id");

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("type", equalTo("LAMAL"))
                .body("franchise", equalTo(2500));
    }

    @Test
    @DisplayName("SCENARIO: A LAA has no franchise")
    void shouldReturnNoFranchiseForLaa() {
        Client company = aCompany();

        createContract("""
                { "clientId": "%s", "type": "LAA", "costAmount": "250.00" }
                """.formatted(company.id()))
                .statusCode(201)
                .body("franchise", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: A LAMal without franchise is refused")
    void shouldRequireFranchiseForLamal() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "costAmount": "250.00" }
                """.formatted(adult.id()))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"));
    }

    @Test
    @DisplayName("SCENARIO: A franchise on a LAA is refused")
    void shouldRefuseFranchiseForLaa() {
        Client company = aCompany();

        createContract("""
                { "clientId": "%s", "type": "LAA", "franchise": 300, "costAmount": "250.00" }
                """.formatted(company.id()))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"));
    }

    @Test
    @DisplayName("SCENARIO: An amount outside the legal franchises is refused")
    void shouldRefuseAmountOutsideLegalFranchises() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 750, "costAmount": "250.00" }
                """.formatted(adult.id()))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"))
                .body("detail", containsString("750"));
    }

    @Test
    @DisplayName("SCENARIO: A decimal franchise is refused instead of being truncated")
    void shouldRefuseDecimalFranchise() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 300.5, "costAmount": "250.00" }
                """.formatted(adult.id()))
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("detail", containsString("franchise"));
    }

    @Test
    @DisplayName("SCENARIO: An adult cannot take a child franchise")
    void shouldRefuseChildFranchiseForAdult() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 0, "costAmount": "250.00" }
                """.formatted(adult.id()))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"));
    }

    @Test
    @DisplayName("SCENARIO: A child LAMal with a child franchise ends at the latest on January 1st of the year they turn 19")
    void shouldCreateChildLamalEndingWithChildhood() {
        LocalDate birthDate = today.toLocalDate().minusYears(10);
        Client child = aPerson(birthDate);

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 0, "endDate": "%s", "costAmount": "100.00" }
                """.formatted(child.id(), iso(endOfChildhood(birthDate))))
                .statusCode(201)
                .body("franchise", equalTo(0));
    }

    @Test
    @DisplayName("SCENARIO: An open-ended child LAMal is refused")
    void shouldRefuseOpenEndedChildLamal() {
        LocalDate birthDate = today.toLocalDate().minusYears(10);
        Client child = aPerson(birthDate);

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 0, "costAmount": "100.00" }
                """.formatted(child.id()))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"))
                .body("detail", containsString(endOfChildhood(birthDate).toLocalDate().toString()));
    }

    @Test
    @DisplayName("SCENARIO: A child cannot take an adult franchise")
    void shouldRefuseAdultFranchiseForChild() {
        LocalDate birthDate = today.toLocalDate().minusYears(10);
        Client child = aPerson(birthDate);

        createContract("""
                { "clientId": "%s", "type": "LAMAL", "franchise": 1000, "endDate": "%s", "costAmount": "100.00" }
                """.formatted(child.id(), iso(endOfChildhood(birthDate))))
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"));
    }

    @Test
    @DisplayName("SCENARIO: Removing the end date of a child LAMal is refused and the period is kept")
    void shouldRefuseOpenEndedChildLamalOnPeriodChange() {
        LocalDate birthDate = today.toLocalDate().minusYears(10);
        Client child = aPerson(birthDate);
        LocalDateTime end = endOfChildhood(birthDate);
        String contractId = createContract(
                """
                        { "clientId": "%s", "type": "LAMAL", "franchise": 0, "startDate": "%s", "endDate": "%s", "costAmount": "100.00" }
                        """
                        .formatted(child.id(), iso(today), iso(end)))
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .body("""
                        { "startDate": "%s", "endDate": null }
                        """.formatted(iso(today)))
                .header(HttpHeaders.IF_MATCH, ETags.current(ContractEndpoints.CONTRACT_BY_ID, contractId))
                .when()
                .put(ContractEndpoints.CONTRACT_PERIOD, contractId)
                .then()
                .statusCode(422)
                .body("code", equalTo("invalidContractFranchise"));

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                .then()
                .statusCode(200)
                .body("endDate", equalTo(iso(end)));
    }

    @Test
    @DisplayName("SCENARIO: An import line with an invalid franchise rejects the whole import")
    void shouldRejectImportLineWithInvalidFranchise() {
        Client adult = aPerson(LocalDate.of(1985, 3, 20));

        given()
                .body("""
                        { "contracts": [
                            { "clientId": "%s", "type": "LAMAL", "franchise": 300, "costAmount": "250.00" },
                            { "clientId": "%s", "type": "LAMAL", "franchise": 100, "startDate": "%s", "costAmount": "250.00" }
                        ] }
                        """
                        .formatted(adult.id(), adult.id(), iso(today.plusYears(5))))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORTS)
                .then()
                .statusCode(422)
                .body("code", equalTo("contractImportRejected"))
                .body("rejections[0].index", equalTo(1))
                .body("rejections[0].code", equalTo("invalidContractFranchise"));
    }

    private ValidatableResponse createContract(final String body) {
        return given()
                .body(body)
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then();
    }

    private Client aPerson(final LocalDate birthDate) {
        return clientRepository.save(ClientFixtures.newPerson("Noah Keller",
                TestDataHelper.uniqueEmail("noah.keller"),
                TestDataHelper.randomSwissPhoneNumber(),
                birthDate,
                LocalDate.now(clock)));
    }

    private Client aCompany() {
        return clientRepository.save(ClientFixtures.newCompany("Acme SA",
                TestDataHelper.uniqueEmail("acme"),
                TestDataHelper.randomSwissPhoneNumber(),
                TestDataHelper.uniqueCompanyIdentifier("CHE")));
    }

    private static LocalDateTime endOfChildhood(final LocalDate birthDate) {
        return LocalDate.of(birthDate.getYear() + 19, 1, 1).atStartOfDay();
    }

    private static String iso(final LocalDateTime date) {
        return date.format(ISO_SECONDS);
    }
}
