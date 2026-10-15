package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.MutableClock;
import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, ContractTotalOverTimeAcceptanceTest.MutableClockConfiguration.class})
@DisplayName("Sum of active contracts over time - Acceptance Tests")
class ContractTotalOverTimeAcceptanceTest {

    private static final Instant REFERENCE_INSTANT = Instant.parse("2026-06-01T10:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(REFERENCE_INSTANT, ZoneOffset.UTC);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MutableClock clock;

    private Client testClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        clock.setInstant(REFERENCE_INSTANT);

        testClient = clientRepository.save(Person.of(
                ClientName.of("Time Travel Client"),
                ClientEmail.of(TestDataHelper.uniqueEmail("time.travel")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20), LocalDate.now(clock))
        ));
    }

    @Test
    @DisplayName("GIVEN a contract expiring without any write WHEN time passes its end date THEN the sum no longer includes it")
    void shouldExcludeContractFromSumOnceItExpiresWithoutAnyWrite() {
        LocalDateTime now = LocalDateTime.now(clock);
        createContract(now.minusDays(10), null, "1000.00");
        createContract(now.minusDays(10), now.plusDays(1), "250.00");

        assertTotal("1250.00");

        clock.advance(Duration.ofDays(2));

        assertTotal("1000.00");
    }

    private void createContract(final LocalDateTime startDate, final LocalDateTime endDate, final String costAmount) {
        String payload = String.format("""
                {
                    "clientId": "%s",
                    "startDate": "%s",
                    "endDate": %s,
                    "costAmount": "%s"
                }
                """, testClient.getId(), startDate, endDate == null ? "null" : "\"" + endDate + "\"", costAmount);

        given().contentType(ContentType.JSON).body(payload)
                .when().post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201);
    }

    private void assertTotal(final String expectedTotal) {
        given()
                .when()
                .get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", testClient.getId())
                .then()
                .statusCode(200)
                .body(equalTo(expectedTotal));
    }
}
