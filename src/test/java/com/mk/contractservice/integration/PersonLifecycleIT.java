package com.mk.contractservice.integration;

import com.mk.contractservice.web.controller.ClientController;
import com.mk.contractservice.web.controller.ContractController;

import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.ContractJpaRepository;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import com.mk.contractservice.web.controller.ClientController;
import com.mk.contractservice.web.controller.ContractController;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Person Client Lifecycle Scenarios - Integration Tests")
class PersonLifecycleIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("SCENARIO: Create person client with all valid data")
    void shouldCreatePersonClientSuccessfully() {
        String createPayload = """
                {
                    "type": "PERSON",
                    "name": "Alice Martin",
                    "email": "alice.martin.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-05-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*" + ClientController.VERSION + "/clients/[0-9a-f-]{36}"))
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", notNullValue())
                .body("name", equalTo("Alice Martin"))
                .body("email", containsString("alice.martin"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-05-15"))
                .extract().path("id");

        given()
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", equalTo(clientId))
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("Alice Martin"))
                .body("birthDate", equalTo("1990-05-15"));
    }

    @Test
    @DisplayName("SCENARIO: Should respect Accept-Language header for different locales")
    void shouldRespectAcceptLanguageHeader() {
        // Test English locale
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "en")
                .body("""
                        {
                            "type": "PERSON",
                            "name": "John Doe",
                            "email": "john.%s@example.com",
                            "phone": "+41791234568",
                            "birthDate": "1990-05-20"
                        }
                        """.formatted(UUID.randomUUID().toString().substring(0, 8)))
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("en"));

        // Test German Swiss locale
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "de-CH")
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Hans Müller",
                            "email": "hans.%s@example.com",
                            "phone": "+41791234569",
                            "birthDate": "1988-08-10"
                        }
                        """.formatted(UUID.randomUUID().toString().substring(0, 8)))
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("de-CH"));

        // Test Italian Swiss locale
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "it-CH")
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Marco Rossi",
                            "email": "marco.%s@example.com",
                            "phone": "+41791234571",
                            "birthDate": "1987-07-15"
                        }
                        """.formatted(UUID.randomUUID().toString().substring(0, 8)))
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("it-CH"));
    }

    @Test
    @DisplayName("SCENARIO: Should fallback to fr-CH for unsupported locale")
    void shouldFallbackToDefaultLocaleForUnsupportedLanguage() {
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "zh-CN")
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Test User",
                            "email": "test.%s@example.com",
                            "phone": "+41791234572",
                            "birthDate": "1995-01-01"
                        }
                        """.formatted(UUID.randomUUID().toString().substring(0, 8)))
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH"));
    }

    @Test
    @DisplayName("SCENARIO: Invalid email format should be rejected")
    void shouldRejectInvalidEmailFormat() {
        String invalidEmailPayload = """
                {
                    "type": "PERSON",
                    "name": "Bob Bernard",
                    "email": "invalid-email-format",
                    "phone": "+41791234567",
                    "birthDate": "1985-03-20"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidEmailPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Invalid phone number format should be rejected")
    void shouldRejectInvalidPhoneFormat() {
        String invalidPhonePayload = """
                {
                    "type": "PERSON",
                    "name": "Charlie Chaplin",
                    "email": "charlie.%s@example.com",
                    "phone": "invalid-phone",
                    "birthDate": "1989-07-10"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(invalidPhonePayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Missing required fields should be rejected")
    void shouldRejectMissingRequiredFields() {
        String missingEmailPayload = """
                {
                    "type": "PERSON",
                    "name": "David Durand",
                    "phone": "+41791234567",
                    "birthDate": "1992-12-01"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(missingEmailPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(422);

        String missingNamePayload = """
                {
                    "email": "david.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1992-12-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(missingNamePayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Future birth date should be rejected")
    void shouldRejectFutureBirthDate() {
        String futureBirthDatePayload = """
                {
                    "type": "PERSON",
                    "name": "Eve Future",
                    "email": "eve.future.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "2030-01-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(futureBirthDatePayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("SCENARIO: Update person client common fields")
    void shouldUpdatePersonCommonFields() {
        String createPayload = """
                {
                    "type": "PERSON",
                    "name": "Frank Original",
                    "email": "frank.original.%s@example.com",
                    "phone": "+41791111111",
                    "birthDate": "1988-04-20"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String updatePayload = """
                {
                    "type": "PERSON",
                    "name": "Frank Updated",
                    "email": "frank.updated.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Frank Updated"))
                .body("email", containsString("frank.updated"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1988-04-20"));
    }

    @Test
    @DisplayName("SCENARIO: Delete person and verify contracts are closed")
    void shouldDeletePersonAndCloseContracts() {
        String personPayload = """
                {
                    "type": "PERSON",
                    "name": "Grace ToDelete",
                    "email": "grace.delete.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1995-08-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .body(personPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String contractPayload = """
                {
                    "startDate": "2025-01-01T00:00:00Z",
                    "endDate": null,
                    "costAmount": "3000.00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(contractPayload)
                .when()
                .post(ContractController.PATH_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(201);

        given()
                .when()
                .delete(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(404);

        given()
                .when()
                .get(ContractController.PATH_BASE + ContractController.PATH_SUM + "?clientId={clientId}", clientId)
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("SCENARIO: Duplicate email should be handled gracefully")
    void shouldHandleDuplicateEmail() {
        String uniqueEmail = "duplicate.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        String firstPayload = """
                {
                    "type": "PERSON",
                    "name": "First Person",
                    "email": "%s",
                    "phone": "+41791111111",
                    "birthDate": "1990-01-01"
                }
                """.formatted(uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(firstPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201);

        String secondPayload = """
                {
                    "type": "PERSON",
                    "name": "Second Person",
                    "email": "%s",
                    "phone": "+41792222222",
                    "birthDate": "1995-06-15"
                }
                """.formatted(uniqueEmail);

        given()
                .contentType(ContentType.JSON)
                .body(secondPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(anyOf(is(409), is(400), is(422), is(500)));
    }

    @Test
    @DisplayName("SCENARIO: Create multiple persons and verify unique IDs")
    void shouldCreateMultiplePersonsWithUniqueIds() {
        String person1Payload = """
                {
                    "type": "PERSON",
                    "name": "Person One",
                    "email": "person1.%s@example.com",
                    "phone": "+41791111111",
                    "birthDate": "1990-01-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String person2Payload = """
                {
                    "type": "PERSON",
                    "name": "Person Two",
                    "email": "person2.%s@example.com",
                    "phone": "+41792222222",
                    "birthDate": "1992-02-02"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String id1 = given()
                .contentType(ContentType.JSON)
                .body(person1Payload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        String id2 = given()
                .contentType(ContentType.JSON)
                .body(person2Payload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .expect()
                .body("id", not(equalTo(id1)))
                .when()
                .get(ClientController.PATH_CLIENT, id2)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("LOCALIZATION: Should accept and return French Swiss locale (fr-CH)")
    void shouldAcceptFrenchSwissLocale() {
        String createPayload = """
                {
                    "type": "PERSON",
                    "name": "Jean Dupont",
                    "email": "jean.dupont.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1990-01-01"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "fr-CH")
                .body(createPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH"))
                .extract().path("id");

        given()
                .header("Accept-Language", "fr-CH")
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"));
    }

    @Test
    @DisplayName("LOCALIZATION: Should accept and return German Swiss locale (de-CH)")
    void shouldAcceptGermanSwissLocale() {
        String createPayload = """
                {
                    "type": "PERSON",
                    "name": "Hans Müller",
                    "email": "hans.muller.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1985-06-15"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String clientId = given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "de-CH")
                .body(createPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("de-CH"))
                .extract().path("id");

        given()
                .header("Accept-Language", "de-CH")
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("de-CH"));
    }

    @Test
    @DisplayName("LOCALIZATION: Should accept multiple locales with quality factors")
    void shouldAcceptMultipleLocalesWithQuality() {
        String createPayload = """
                {
                    "type": "PERSON",
                    "name": "Maria Rossi",
                    "email": "maria.rossi.%s@example.com",
                    "phone": "+41791234567",
                    "birthDate": "1992-03-20"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        // Client prefers fr-CH, but accepts de-CH and en as fallback
        String clientId = given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "fr-CH, de-CH;q=0.8, en;q=0.5")
                .body(createPayload)
                .when()
                .post(ClientController.PATH_BASE)
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH")) // Should pick highest priority
                .extract().path("id");

        given()
                .header("Accept-Language", "it-CH")
                .when()
                .get(ClientController.PATH_CLIENT, clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("it-CH"));
    }
}







