package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.ETags;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: a client held by an import batch for too long answers busy instead of waiting forever")
class ClientBusyDuringImportBatchAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Client client;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        final LocalDateTime now = LocalDateTime.now(clock);
        client = clientRepository.save(Person.of(
                ClientName.of("Busy Client"),
                ClientEmail.of(TestDataHelper.uniqueEmail("busy.client")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
    }

    private ValidatableResponse whileAnImportBatchHoldsTheClient(final Supplier<ValidatableResponse> request) {
        final TransactionTemplate batch = new TransactionTemplate(transactionManager);
        batch.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return batch.execute(status -> {
            assertThat(clientRepository.lockExistingIds(Set.of(client.id()))).containsExactly(client.id());
            return request.get();
        });
    }

    @Test
    @DisplayName("GIVEN an import batch holding the client longer than the lock timeout WHEN the client is updated THEN 409 resourceBusy and the client is unchanged")
    void shouldAnswerBusyWhenUpdatingClientHeldByImportBatch() {
        whileAnImportBatchHoldsTheClient(() -> given()
                .header(HttpHeaders.IF_MATCH, ETags.ofVersion(0))
                .body("""
                        {
                            "name": "Updated Name",
                            "email": "%s",
                            "phone": "%s"
                        }
                        """.formatted(client.email().value(), client.phone().value()))
                .put(ClientEndpoints.CLIENT_BY_ID, client.id())
                .then())
                .statusCode(409)
                .body("code", equalTo("resourceBusy"));

        given()
                .get(ClientEndpoints.CLIENT_BY_ID, client.id())
                .then()
                .statusCode(200)
                .body("name", equalTo("Busy Client"));
    }

    @Test
    @DisplayName("GIVEN an import batch holding the client longer than the lock timeout WHEN the client is deleted THEN 409 resourceBusy and the client still exists")
    void shouldAnswerBusyWhenDeletingClientHeldByImportBatch() {
        whileAnImportBatchHoldsTheClient(() -> given()
                .delete(ClientEndpoints.CLIENT_BY_ID, client.id())
                .then())
                .statusCode(409)
                .body("code", equalTo("resourceBusy"));

        given()
                .get(ClientEndpoints.CLIENT_BY_ID, client.id())
                .then()
                .statusCode(200);
    }
}
