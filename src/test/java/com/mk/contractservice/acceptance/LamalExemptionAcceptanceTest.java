package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.ETags;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.shared.Residence;
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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("LAMal exemption of cross-border workers - Acceptance Tests")
class LamalExemptionAcceptanceTest {

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

    private LocalDate today;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        today = LocalDate.now(clock);
    }

    @Test
    @DisplayName("SCENARIO: A counselor records the exemption of a cross-border worker and reads it back")
    void shouldRecordAndReadExemption() {
        UUID frontalier = aPersonLivingIn(Residence.FR);

        recordExemption(frontalier, """
                { "startDate": "%s" }
                """.formatted(today.minusMonths(2)))
                .statusCode(204);

        given()
                .get(ClientEndpoints.CLIENT_LAMAL_EXEMPTION, frontalier)
                .then()
                .statusCode(200)
                .body("startDate", equalTo(today.minusMonths(2).toString()))
                .body("endDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: A LAMal is refused while its holder is exempted from LAMal")
    void shouldRefuseLamalDuringExemption() {
        UUID frontalier = aPersonLivingIn(Residence.DE);
        recordExemption(frontalier, """
                { "startDate": "%s" }
                """.formatted(today.minusMonths(2)))
                .statusCode(204);

        given()
                .body("""
                        { "clientId": "%s", "type": "LAMAL", "franchise": 300, "costAmount": "350.00" }
                        """.formatted(frontalier))
                .when()
                .post(ContractEndpoints.CONTRACTS_BASE)
                .then()
                .statusCode(409)
                .body("code", equalTo("lamalExemptionConflict"))
                .body("detail", containsString("is exempted from LAMal"));
    }

    @Test
    @DisplayName("SCENARIO: An exempted worker moves to Switzerland only once the exemption is closed")
    void shouldMoveToSwitzerlandOnceExemptionIsClosed() {
        UUID frontalier = aPersonLivingIn(Residence.IT);
        recordExemption(frontalier, """
                { "startDate": "%s" }
                """.formatted(today.minusMonths(2)))
                .statusCode(204);

        moveTo(frontalier, Residence.CH)
                .statusCode(409)
                .body("code", equalTo("lamalExemptionOngoing"));

        recordExemption(frontalier, """
                { "startDate": "%s", "endDate": "%s" }
                """.formatted(today.minusMonths(2), today))
                .statusCode(204);
        moveTo(frontalier, Residence.CH)
                .statusCode(204);

        given()
                .get(ClientEndpoints.CLIENT_BY_ID, frontalier)
                .then()
                .statusCode(200)
                .body("residence", equalTo("CH"));
    }

    @Test
    @DisplayName("SCENARIO: A person living in Switzerland cannot be exempted from LAMal")
    void shouldRefuseExemptionOfResident() {
        UUID resident = aPersonLivingIn(Residence.CH);

        recordExemption(resident, """
                { "startDate": "%s" }
                """.formatted(today))
                .statusCode(422)
                .body("code", equalTo("lamalExemptionNotAllowed"));
    }

    @Test
    @DisplayName("SCENARIO: Reading the exemption of a person who has none answers 404")
    void shouldAnswerNotFoundWithoutExemption() {
        UUID frontalier = aPersonLivingIn(Residence.AT);

        given()
                .get(ClientEndpoints.CLIENT_LAMAL_EXEMPTION, frontalier)
                .then()
                .statusCode(404)
                .body("code", equalTo("lamalExemptionNotFound"));
    }

    @Test
    @DisplayName("SCENARIO: An exemption without start date is refused")
    void shouldRequireStartDate() {
        UUID frontalier = aPersonLivingIn(Residence.FR);

        recordExemption(frontalier, """
                { "endDate": "%s" }
                """.formatted(today))
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("validations[0].field", equalTo("startDate"));
    }

    private ValidatableResponse recordExemption(final UUID clientId, final String body) {
        return given()
                .body(body)
                .when()
                .put(ClientEndpoints.CLIENT_LAMAL_EXEMPTION, clientId)
                .then();
    }

    private ValidatableResponse moveTo(final UUID clientId, final Residence residence) {
        return given()
                .header(HttpHeaders.IF_MATCH, ETags.current(ClientEndpoints.CLIENT_BY_ID, clientId))
                .body("""
                        { "residence": "%s" }
                        """.formatted(residence))
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then();
    }

    private UUID aPersonLivingIn(final Residence residence) {
        Client person = clientRepository.save(ClientFixtures.newPerson("Lena Brunner",
                TestDataHelper.uniqueEmail("lena.brunner"),
                TestDataHelper.randomSwissPhoneNumber(),
                LocalDate.of(1985, 3, 20),
                today,
                residence));
        return person.id();
    }
}
