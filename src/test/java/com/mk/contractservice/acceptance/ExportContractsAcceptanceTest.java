package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.MutableInstantSource;
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
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import io.restassured.path.json.JsonPath;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ExportContractsAcceptanceTest.MutableInstantSourceConfiguration.class})
@DisplayName("Export of active contracts - Acceptance Tests")
class ExportContractsAcceptanceTest {

    private static final Instant REFERENCE_INSTANT = Instant.parse("2026-06-01T10:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableInstantSourceConfiguration {

        @Bean
        @Primary
        MutableInstantSource mutableInstantSource() {
            return new MutableInstantSource(REFERENCE_INSTANT);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private MutableInstantSource instantSource;

    @Autowired
    private Clock clock;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        instantSource.setInstant(REFERENCE_INSTANT);
        now = LocalDateTime.now(clock);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("GIVEN active and expired contracts of several clients WHEN following the cursor to the end THEN every active contract is exported exactly once")
    void shouldExportEveryActiveContractOfEveryClientExactlyOnce() {
        final Client alice = createClient("alice");
        final Client bob = createClient("bob");
        final List<String> activeContractIds = List.of(
                createContract(alice, null),
                createContract(alice, now.plusYears(1)),
                createContract(alice, null),
                createContract(bob, null),
                createContract(bob, now.plusDays(1))
        );
        createContract(alice, now.minusDays(1));

        final JsonPath firstPage = exportPage(null, 2);
        final JsonPath secondPage = exportPage(firstPage.getString("nextCursor"), 2);
        final JsonPath lastPage = exportPage(secondPage.getString("nextCursor"), 2);

        assertThat(firstPage.getList("items")).hasSize(2);
        assertThat(secondPage.getList("items")).hasSize(2);
        assertThat(lastPage.getList("items")).hasSize(1);
        assertThat(lastPage.getString("nextCursor")).isNull();
        assertThat(idsOf(firstPage, secondPage, lastPage)).containsExactlyInAnyOrderElementsOf(activeContractIds);
    }

    @Test
    @DisplayName("GIVEN contracts whose cost changes during the export WHEN following the cursor THEN no contract is skipped or exported twice")
    void shouldNotSkipNorRepeatContractsModifiedDuringExport() {
        final Client client = createClient("modified.during.export");
        final List<String> contractIds = List.of(
                createContract(client, null),
                createContract(client, null),
                createContract(client, null),
                createContract(client, null)
        );

        final JsonPath firstPage = exportPage(null, 2);
        final String alreadyExported = firstPage.getString("items[0].id");
        final String notYetExported = contractIds.stream()
                .filter(id -> !firstPage.getList("items.id", String.class).contains(id))
                .findFirst()
                .orElseThrow();
        instantSource.advance(Duration.ofMinutes(1));
        changeCost(alreadyExported, "999.00");
        changeCost(notYetExported, "888.00");
        final JsonPath lastPage = exportPage(firstPage.getString("nextCursor"), 2);

        assertThat(lastPage.getString("nextCursor")).isNull();
        assertThat(idsOf(firstPage, lastPage)).containsExactlyInAnyOrderElementsOf(contractIds);
    }

    @Test
    @DisplayName("GIVEN a contract not yet exported WHEN it expires during the export THEN the following pages no longer contain it")
    void shouldNotExportContractExpiredBeforeItsPageIsRead() {
        final Client client = createClient("expires.during.export");
        createContract(client, now.plusHours(1));
        createContract(client, now.plusHours(1));
        createContract(client, now.plusHours(1));

        final JsonPath firstPage = exportPage(null, 2);
        instantSource.advance(Duration.ofHours(2));
        final JsonPath lastPage = exportPage(firstPage.getString("nextCursor"), 2);

        assertThat(firstPage.getList("items")).hasSize(2);
        assertThat(lastPage.getList("items")).isEmpty();
        assertThat(lastPage.getString("nextCursor")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-cursor!", "YWJj", "abc"})
    @DisplayName("GIVEN a cursor not issued by the API WHEN exporting THEN return 400 invalidCursor")
    void shouldRejectCursorNotIssuedByTheApi(final String cursor) {
        given()
                .queryParam("cursor", cursor)
                .when()
                .get(ContractEndpoints.CONTRACT_EXPORT)
                .then()
                .statusCode(400)
                .body("code", equalTo("invalidCursor"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1001})
    @DisplayName("GIVEN a page size outside 1..1000 WHEN exporting THEN return 400 invalidParameter")
    void shouldRejectPageSizeOutOfBounds(final int size) {
        given()
                .queryParam("size", size)
                .when()
                .get(ContractEndpoints.CONTRACT_EXPORT)
                .then()
                .statusCode(400)
                .body("code", equalTo("invalidParameter"));
    }

    @Test
    @DisplayName("GIVEN no active contract WHEN exporting THEN return an empty last page")
    void shouldReturnEmptyLastPageWhenNothingToExport() {
        given()
                .when()
                .get(ContractEndpoints.CONTRACT_EXPORT)
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(0))
                .body("nextCursor", nullValue());
    }

    private JsonPath exportPage(@Nullable final String cursor, final int size) {
        final var request = given().queryParam("size", size);
        if (cursor != null) {
            request.queryParam("cursor", cursor);
        }
        return request
                .when()
                .get(ContractEndpoints.CONTRACT_EXPORT)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private static List<String> idsOf(final JsonPath... pages) {
        final List<String> ids = new ArrayList<>();
        for (final JsonPath page : pages) {
            ids.addAll(page.getList("items.id", String.class));
        }
        return ids;
    }

    private Client createClient(final String prefix) {
        return clientRepository.save(Person.of(
                ClientName.of("Export Client"),
                ClientEmail.of(TestDataHelper.uniqueEmail(prefix)),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20), LocalDate.now(clock))
        ));
    }

    private String createContract(final Client client, @Nullable final LocalDateTime endDate) {
        final String payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": %s,
                    "costAmount": "100.00"
                }
                """, client.getId(), now.minusMonths(1), endDate == null ? "null" : "\"" + endDate + "\"");

        return given().body(payload)
                .when().post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201)
                .extract().path("id");
    }

    private void changeCost(final String contractId, final String amount) {
        given().body("{\"amount\": " + amount + "}")
                .header(HttpHeaders.IF_MATCH, ETags.current(ContractEndpoints.CONTRACT_BY_ID, UUID.fromString(contractId)))
                .when().patch(ContractEndpoints.CONTRACT_COST, UUID.fromString(contractId))
                .then().statusCode(204);
    }
}
