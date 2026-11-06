package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.features.contract.ImportContracts;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Technical guarantee: an import sends its inserts in JDBC batches")
class ContractBatchInsertAcceptanceTest {

    private static final int CLIENT_EXISTENCE_QUERY = 1;
    private static final int MAX_INSERT_BATCHES = ImportContracts.MAX_LINES / 50;

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void tearDown() {
        statistics.setStatisticsEnabled(false);
    }

    @Test
    @DisplayName("GIVEN the largest import WHEN importing THEN it costs at most one statement per batch, not one per line")
    void shouldSendAtMostOneStatementPerBatch() {
        final String clientId = given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Marie Durand",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "1985-03-20"
                        }
                        """.formatted(TestDataHelper.uniqueEmail("marie.durand"), TestDataHelper.randomSwissPhoneNumber()))
                .when().post(ClientEndpoints.CLIENTS_BASE)
                .then().statusCode(201)
                .extract().path("id");
        final String line = """
                { "clientId": "%s", "costAmount": "10.00" }""".formatted(clientId);
        final String body = """
                { "contracts": [%s] }""".formatted(String.join(",", Collections.nCopies(ImportContracts.MAX_LINES, line)));

        statistics.clear();

        given()
                .body(body)
                .when().post(ContractEndpoints.CONTRACT_IMPORTS)
                .then().statusCode(201);

        assertThat(statistics.getPrepareStatementCount())
                .isLessThanOrEqualTo(CLIENT_EXISTENCE_QUERY + MAX_INSERT_BATCHES);
    }
}
