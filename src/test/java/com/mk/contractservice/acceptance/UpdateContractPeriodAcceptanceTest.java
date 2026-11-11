package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.ETags;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractRepository;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Update Contract Period - Acceptance Tests")
class UpdateContractPeriodAcceptanceTest {

    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private Client testClient;
    private LocalDateTime today;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        testClient = clientRepository.save(Person.of(
                ClientName.of("Lea Meier"),
                ClientEmail.of("lea.meier." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 4, 12), LocalDate.now(clock))
        ));
        today = LocalDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("SCENARIO: Replacing the period persists the new start and end dates")
    void shouldReplacePeriod() {
        Contract contract = givenContract(today.minusMonths(6), today.plusMonths(6));
        LocalDateTime newStart = today.minusYears(1);
        LocalDateTime newEnd = today.plusYears(2);

        putPeriod(contract, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(newStart), iso(newEnd)))
                .statusCode(204);

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract.id())
                .then()
                .statusCode(200)
                .body("startDate", equalTo(iso(newStart)))
                .body("endDate", equalTo(iso(newEnd)))
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("SCENARIO: A LAMal cannot be extended over the next LAMal of the same person")
    void shouldRefusePeriodOverlappingAnotherLamal() {
        Contract current = givenContract(today.minusMonths(6), today.plusMonths(6));
        givenContract(today.plusMonths(6), null);

        putPeriod(current, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(today.minusMonths(6)), iso(today.plusYears(1))))
                .statusCode(409)
                .body("code", equalTo("contractOverlap"));

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, current.id())
                .then()
                .statusCode(200)
                .body("endDate", equalTo(iso(today.plusMonths(6))));
    }

    @Test
    @DisplayName("SCENARIO: A null end date makes the contract open-ended")
    void shouldRemoveEndDate() {
        Contract contract = givenContract(today.minusMonths(6), today.plusMonths(6));

        putPeriod(contract, """
                { "startDate": "%s", "endDate": null }
                """.formatted(iso(today.minusMonths(6))))
                .statusCode(204);

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract.id())
                .then()
                .statusCode(200)
                .body("startDate", equalTo(iso(today.minusMonths(6))))
                .body("endDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: A period change updates the last modification date used by updatedSince")
    void shouldUpdateLastModified() {
        Contract contract = givenContract(today.minusMonths(6), null);
        String updatedSince = LocalDateTime.now(clock).minusHours(1).toString();

        listUpdatedSince(updatedSince).body("totalElements", equalTo(0));

        putPeriod(contract, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(today.minusMonths(6)), iso(today.plusYears(1))))
                .statusCode(204);

        listUpdatedSince(updatedSince).body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("SCENARIO: An end date in the past is rejected with 422 invalidContractPeriod and the period is kept")
    void shouldRejectRetroactiveEndDate() {
        Contract contract = givenContract(today.minusMonths(6), null);

        putPeriod(contract, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(today.minusMonths(6)), iso(today.minusDays(1))))
                .statusCode(422)
                .body("code", equalTo("invalidContractPeriod"));

        given()
                .get(ContractEndpoints.CONTRACT_BY_ID, contract.id())
                .then()
                .body("endDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: A start date after the end date is rejected with 422 invalidContractPeriod")
    void shouldRejectStartDateAfterEndDate() {
        Contract contract = givenContract(today.minusMonths(6), today.plusMonths(6));

        putPeriod(contract, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(today.plusMonths(7)), iso(today.plusMonths(6))))
                .statusCode(422)
                .body("code", equalTo("invalidContractPeriod"));
    }

    @Test
    @DisplayName("SCENARIO: A missing start date is rejected with 422 validationError")
    void shouldRequireStartDate() {
        Contract contract = givenContract(today.minusMonths(6), null);

        putPeriod(contract, """
                { "endDate": "%s" }
                """.formatted(iso(today.plusYears(1))))
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations[0].field", equalTo("startDate"));
    }

    @Test
    @DisplayName("SCENARIO: The period of an expired contract cannot be changed (409 contractExpired)")
    void shouldRejectExpiredContract() {
        Contract contract = givenContract(today.minusMonths(6), today.minusDays(1));

        putPeriod(contract, """
                { "startDate": "%s", "endDate": null }
                """.formatted(iso(today.minusMonths(6))))
                .statusCode(409)
                .body("code", equalTo("contractExpired"));
    }

    @Test
    @DisplayName("SCENARIO: Changing the period without If-Match returns 428 preconditionRequired")
    void shouldRequireIfMatch() {
        Contract contract = givenContract(today.minusMonths(6), null);

        given()
                .body(validBody())
                .when()
                .put(ContractEndpoints.CONTRACT_PERIOD, contract.id())
                .then()
                .statusCode(428)
                .body("code", equalTo("preconditionRequired"));
    }

    @Test
    @DisplayName("SCENARIO: Changing the period with a stale ETag returns 412 preconditionFailed")
    void shouldRejectStaleETag() {
        Contract contract = givenContract(today.minusMonths(6), null);
        String readETag = ETags.current(ContractEndpoints.CONTRACT_BY_ID, contract.id());

        putPeriod(contract, validBody()).statusCode(204);

        given()
                .body("""
                        { "startDate": "%s", "endDate": null }
                        """.formatted(iso(today.minusMonths(6))))
                .header(HttpHeaders.IF_MATCH, readETag)
                .when()
                .put(ContractEndpoints.CONTRACT_PERIOD, contract.id())
                .then()
                .statusCode(412)
                .body("code", equalTo("preconditionFailed"));
    }

    @Test
    @DisplayName("SCENARIO: Changing the period of an unknown contract returns 404 contractNotFound")
    void shouldReturn404ForUnknownContract() {
        given()
                .body(validBody())
                .header(HttpHeaders.IF_MATCH, ETags.ofVersion(0))
                .when()
                .put(ContractEndpoints.CONTRACT_PERIOD, UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("contractNotFound"));
    }

    private String validBody() {
        return """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(iso(today.minusMonths(6)), iso(today.plusYears(1)));
    }

    private Contract givenContract(final LocalDateTime startDate, final LocalDateTime endDate) {
        return contractRepository.save(ContractFixtures.newContract(testClient.id(),
                startDate,
                endDate,
                new BigDecimal("300.00"),
                LocalDateTime.now(clock).minusDays(1)));
    }

    private ValidatableResponse putPeriod(final Contract contract, final String body) {
        return given()
                .body(body)
                .header(HttpHeaders.IF_MATCH, ETags.current(ContractEndpoints.CONTRACT_BY_ID, contract.id()))
                .when()
                .put(ContractEndpoints.CONTRACT_PERIOD, contract.id())
                .then();
    }

    private ValidatableResponse listUpdatedSince(final String updatedSince) {
        return given()
                .queryParam("clientId", testClient.id())
                .queryParam("updatedSince", updatedSince)
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(200);
    }

    private static String iso(final LocalDateTime date) {
        return date.format(ISO_SECONDS);
    }
}
