package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Optimistic Locking with ETag / If-Match - Integration Tests")
class OptimisticLockingAcceptanceTest {

    private static final String CLIENT_PAYLOAD = """
            {
                "name": "%s",
                "email": "%s",
                "phone": "%s"
            }
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    private String clientId;
    private String clientETag;
    private String email;
    private String phone;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        email = TestDataHelper.uniqueEmail("etag");
        phone = TestDataHelper.randomSwissPhoneNumber();
        clientId = given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Alice Martin",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "1990-05-15"
                        }
                        """.formatted(email, phone))
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");
        clientETag = currentClientETag();
    }

    @Nested
    @DisplayName("Reading a client")
    class ReadClient {

        @Test
        @DisplayName("GIVEN a created client WHEN it is read THEN it carries a strong ETag")
        void shouldReturnStrongETagOnRead() {
            assertThat(clientETag).isNotBlank().doesNotStartWith("W/");
        }

        @Test
        @DisplayName("GIVEN the current ETag WHEN the client is read with If-None-Match THEN 304 Not Modified")
        void shouldAnswerNotModifiedWhenETagStillMatches() {
            given()
                    .header(HttpHeaders.IF_NONE_MATCH, clientETag)
                    .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(304);
        }
    }

    @Nested
    @DisplayName("Updating a client")
    class UpdateClient {

        @Test
        @DisplayName("GIVEN no If-Match WHEN the client is updated THEN 428 and the client is unchanged")
        void shouldRequireIfMatchOnUpdate() {
            given()
                    .body(CLIENT_PAYLOAD.formatted("Alice Updated", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(428)
                    .body("code", equalTo("preconditionRequired"));

            assertName("Alice Martin");
        }

        @Test
        @DisplayName("GIVEN If-Match * WHEN the client is updated THEN 428 because the wildcard would bypass the lock")
        void shouldRejectWildcardIfMatch() {
            given()
                    .header(HttpHeaders.IF_MATCH, "*")
                    .body(CLIENT_PAYLOAD.formatted("Alice Updated", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(428)
                    .body("code", equalTo("preconditionRequired"));
        }

        @Test
        @DisplayName("GIVEN the current ETag WHEN the client is updated THEN 204 and its ETag changes")
        void shouldUpdateWithCurrentETag() {
            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body(CLIENT_PAYLOAD.formatted("Alice Updated", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204)
                    .header(HttpHeaders.ETAG, nullValue());

            assertName("Alice Updated");
            assertThat(currentClientETag()).isNotEqualTo(clientETag);
        }

        @Test
        @DisplayName("GIVEN another counselor updated the client since it was read WHEN it is updated with the old ETag THEN 412 and the other change is kept")
        void shouldRejectUpdateBasedOnStaleRead() {
            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body(CLIENT_PAYLOAD.formatted("First Counselor", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204);

            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body(CLIENT_PAYLOAD.formatted("Second Counselor", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(412)
                    .body("code", equalTo("preconditionFailed"));

            assertName("First Counselor");
        }

        @Test
        @DisplayName("GIVEN an unknown client WHEN it is updated with an ETag THEN 404")
        void shouldAnswerNotFoundBeforePrecondition() {
            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body(CLIENT_PAYLOAD.formatted("Alice Updated", email, phone))
                    .put(ClientEndpoints.CLIENT_BY_ID, UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("Patching a client")
    class PatchClient {

        @Test
        @DisplayName("GIVEN no If-Match WHEN the client is patched THEN 428")
        void shouldRequireIfMatchOnPatch() {
            given()
                    .body("""
                            { "name": "Alice Patched" }
                            """)
                    .patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(428)
                    .body("code", equalTo("preconditionRequired"));

            assertName("Alice Martin");
        }

        @Test
        @DisplayName("GIVEN the current ETag WHEN the client is patched THEN 204 and its ETag changes")
        void shouldPatchWithCurrentETag() {
            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body("""
                            { "name": "Alice Patched" }
                            """)
                    .patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204)
                    .header(HttpHeaders.ETAG, nullValue());

            assertName("Alice Patched");
            assertThat(currentClientETag()).isNotEqualTo(clientETag);
        }

        @Test
        @DisplayName("GIVEN identical values WHEN the client is patched THEN its ETag does not change")
        void shouldKeepETagWhenNothingChanges() {
            given()
                    .header(HttpHeaders.IF_MATCH, clientETag)
                    .body("""
                            { "name": "Alice Martin" }
                            """)
                    .patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204);

            assertThat(currentClientETag()).isEqualTo(clientETag);
        }

        @Test
        @DisplayName("GIVEN a weak ETag WHEN the client is patched THEN 412 because If-Match uses the strong comparison")
        void shouldRejectWeakETag() {
            given()
                    .header(HttpHeaders.IF_MATCH, "W/" + clientETag)
                    .body("""
                            { "name": "Alice Patched" }
                            """)
                    .patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(412)
                    .body("code", equalTo("preconditionFailed"));

            assertName("Alice Martin");
        }
    }

    @Nested
    @DisplayName("Changing the cost of a contract")
    class PatchContractCost {

        private String contractId;
        private String contractETag;

        @BeforeEach
        void setUp() {
            contractId = given()
                    .body("""
                            {
                                "clientId": "%s",
                                "startDate": "2025-01-01T00:00:00",
                                "costAmount": "1000.00"
                            }
                            """.formatted(clientId))
                    .post(ContractEndpoints.CONTRACTS_BASE)
                    .then()
                    .statusCode(201)
                    .extract().path("id");
            contractETag = currentContractETag();
        }

        @Test
        @DisplayName("GIVEN a created contract WHEN it is read THEN it carries a strong ETag")
        void shouldReturnStrongETagOnRead() {
            assertThat(contractETag).isNotBlank().doesNotStartWith("W/");
        }

        @Test
        @DisplayName("GIVEN the current ETag WHEN the contract is read with If-None-Match THEN 304 Not Modified")
        void shouldAnswerNotModifiedWhenETagStillMatches() {
            given()
                    .header(HttpHeaders.IF_NONE_MATCH, contractETag)
                    .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                    .then()
                    .statusCode(304);
        }

        @Test
        @DisplayName("GIVEN no If-Match WHEN the cost is changed THEN 428")
        void shouldRequireIfMatch() {
            given()
                    .body("""
                            { "amount": "1500.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, contractId)
                    .then()
                    .statusCode(428)
                    .body("code", equalTo("preconditionRequired"));

            assertCost(1000.00f);
        }

        @Test
        @DisplayName("GIVEN the current ETag WHEN the cost is changed THEN 204 and the ETag of the contract changes")
        void shouldChangeCostWithCurrentETag() {
            given()
                    .header(HttpHeaders.IF_MATCH, contractETag)
                    .body("""
                            { "amount": "1500.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, contractId)
                    .then()
                    .statusCode(204)
                    .header(HttpHeaders.ETAG, nullValue());

            assertCost(1500.00f);
            assertThat(currentContractETag()).isNotEqualTo(contractETag);
        }

        @Test
        @DisplayName("GIVEN another counselor changed the cost since it was read WHEN the cost is changed with the old ETag THEN 412 and the other cost is kept")
        void shouldRejectCostChangeBasedOnStaleRead() {
            given()
                    .header(HttpHeaders.IF_MATCH, contractETag)
                    .body("""
                            { "amount": "1200.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, contractId)
                    .then()
                    .statusCode(204);

            given()
                    .header(HttpHeaders.IF_MATCH, contractETag)
                    .body("""
                            { "amount": "1500.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, contractId)
                    .then()
                    .statusCode(412)
                    .body("code", equalTo("preconditionFailed"));

            assertCost(1200.00f);
        }

        @Test
        @DisplayName("GIVEN the client was deleted since the contract was read WHEN the cost is changed with the old ETag THEN 412 and the contract stays closed")
        void shouldRejectCostChangeWhenContractWasClosedByClientDeletion() {
            given()
                    .delete(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204);

            given()
                    .header(HttpHeaders.IF_MATCH, contractETag)
                    .body("""
                            { "amount": "1500.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, contractId)
                    .then()
                    .statusCode(412);

            assertCost(1000.00f);
            assertThat(contractJpaRepository.findById(UUID.fromString(contractId)).orElseThrow().getEndDate()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN an unknown contract WHEN its cost is changed with an ETag THEN 404")
        void shouldAnswerNotFoundBeforePrecondition() {
            given()
                    .header(HttpHeaders.IF_MATCH, contractETag)
                    .body("""
                            { "amount": "1500.00" }
                            """)
                    .patch(ContractEndpoints.CONTRACT_COST, UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }

        private String currentContractETag() {
            return given()
                    .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                    .then()
                    .statusCode(200)
                    .extract().header(HttpHeaders.ETAG);
        }

        private void assertCost(final float expectedCost) {
            given()
                    .get(ContractEndpoints.CONTRACT_BY_ID, contractId)
                    .then()
                    .statusCode(200)
                    .body("costAmount", equalTo(expectedCost));
        }
    }

    private String currentClientETag() {
        return given()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(200)
                .extract().header(HttpHeaders.ETAG);
    }

    private void assertName(final String expectedName) {
        given()
                .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo(expectedName));
    }
}
