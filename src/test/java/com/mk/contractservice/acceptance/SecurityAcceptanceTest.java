package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.acceptance.helper.TestTokens;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: Only counselors can use the API")
class SecurityAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        RestAssured.authentication = RestAssured.DEFAULT_AUTH;

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    static Stream<Arguments> rejectedTokens() {
        return Stream.of(
                Arguments.of("expired", TestTokens.builder().realmRoles(TestTokens.COUNSELOR_ROLE)
                        .expiresAt(Instant.now().minusSeconds(120)).build()),
                Arguments.of("issued for another application", TestTokens.builder().realmRoles(TestTokens.COUNSELOR_ROLE)
                        .audience("another-application").build()),
                Arguments.of("issued by another realm", TestTokens.builder().realmRoles(TestTokens.COUNSELOR_ROLE)
                        .issuer("https://keycloak.test/realms/another-realm").build()),
                Arguments.of("with a tampered signature", TestTokens.counselor() + "tampered")
        );
    }

    @Test
    @DisplayName("GIVEN no token WHEN reading a client THEN 401 unauthenticated")
    void shouldRejectRequestWithoutToken() {
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, UUID.randomUUID())
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", startsWith("Bearer"))
                .contentType("application/problem+json")
                .body("code", equalTo("unauthenticated"));
    }

    @ParameterizedTest(name = "token {0}")
    @MethodSource("rejectedTokens")
    @DisplayName("GIVEN an invalid token WHEN reading a client THEN 401 unauthenticated")
    void shouldRejectInvalidToken(String description, String token) {
        given()
                .auth().oauth2(token)
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, UUID.randomUUID())
                .then()
                .statusCode(401)
                .body("code", equalTo("unauthenticated"));
    }

    @Test
    @DisplayName("GIVEN no token and an invalid payload WHEN creating a client THEN 401 before any validation")
    void shouldAuthenticateBeforeValidatingPayload() {
        given()
                .body("{}")
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(401)
                .body("code", equalTo("unauthenticated"));
    }

    @Test
    @DisplayName("GIVEN a user without the counselor role WHEN reading a client THEN 403 accessDenied")
    void shouldRejectUserWithoutCounselorRole() {
        given()
                .auth().oauth2(TestTokens.builder().realmRoles("offline_access").build())
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, UUID.randomUUID())
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("code", equalTo("accessDenied"));
    }

    @Test
    @DisplayName("GIVEN a counselor role granted by another Keycloak client WHEN reading a client THEN 403 accessDenied")
    void shouldIgnoreRolesOfOtherKeycloakClients() {
        given()
                .auth().oauth2(TestTokens.builder().clientRoles("another-application", TestTokens.COUNSELOR_ROLE).build())
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, UUID.randomUUID())
                .then()
                .statusCode(403)
                .body("code", equalTo("accessDenied"));
    }

    @Test
    @DisplayName("GIVEN a counselor WHEN calling a route outside the API THEN 403 accessDenied")
    void shouldDenyRoutesOutsideTheApi() {
        given()
                .auth().oauth2(TestTokens.counselor())
                .when()
                .get("/actuator/env")
                .then()
                .statusCode(403)
                .body("code", equalTo("accessDenied"));
    }

    @Test
    @DisplayName("GIVEN no token WHEN checking health THEN 200")
    void shouldExposeHealthWithoutToken() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GIVEN a counselor WHEN managing a client and its contract THEN every endpoint answers successfully")
    void shouldLetCounselorUseEveryEndpoint() {
        RestAssured.authentication = RestAssured.oauth2(TestTokens.counselor());
        final String email = TestDataHelper.uniqueEmail("counselor.flow");
        final String phone = TestDataHelper.randomSwissPhoneNumber();

        final String clientId = given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Counselor Flow",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "1990-01-01"
                        }
                        """.formatted(email, phone))
                .when().post(ClientEndpoints.CLIENTS_BASE)
                .then().statusCode(201)
                .extract().path("id");

        given().when().get(ClientEndpoints.CLIENT_BY_ID, clientId).then().statusCode(200);

        given()
                .body("""
                        {
                            "name": "Counselor Flow Updated",
                            "email": "%s",
                            "phone": "%s"
                        }
                        """.formatted(email, phone))
                .when().put(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then().statusCode(204);

        given()
                .body("""
                        { "name": "Counselor Flow Patched" }
                        """)
                .when().patch(ClientEndpoints.CLIENT_BY_ID, clientId)
                .then().statusCode(204);

        final String contractId = given()
                .body("""
                        {
                            "clientId": "%s",
                            "costAmount": "1000.00"
                        }
                        """.formatted(clientId))
                .when().post(ContractEndpoints.CONTRACTS_BASE)
                .then().statusCode(201)
                .extract().path("id");

        given().when().get(ContractEndpoints.CONTRACT_BY_ID, contractId).then().statusCode(200);

        given()
                .body("""
                        { "amount": "1200.00" }
                        """)
                .when().patch(ContractEndpoints.CONTRACT_COST, contractId)
                .then().statusCode(204);

        given().when().get(ContractEndpoints.CONTRACTS_BASE + "?clientId={clientId}", clientId).then().statusCode(200);

        given().when().get(ContractEndpoints.CONTRACTS_BASE + ContractEndpoints.CONTRACT_TOTAL + "?clientId={clientId}", clientId)
                .then().statusCode(200);

        given().when().delete(ClientEndpoints.CLIENT_BY_ID, clientId).then().statusCode(204);
    }
}
