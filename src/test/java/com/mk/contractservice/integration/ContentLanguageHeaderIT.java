package com.mk.contractservice.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Content-Language Header Tests")
class ContentLanguageHeaderIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("contract_test")
            .withUsername("test")
            .withPassword("test");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Should return Content-Language: fr-CH by default when creating a client")
    void shouldReturnFrenchSwissLocaleByDefaultOnClientCreation() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "type": "PERSON",
                          "name": "Jean Dupont",
                          "email": "jean.locale.test@example.com",
                          "phone": "+41791234567",
                          "birthDate": "1985-03-15"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH"))
                .header("Location", notNullValue());
    }

    @Test
    @DisplayName("Should return Content-Language: en when Accept-Language: en is provided")
    void shouldReturnEnglishLocaleWhenRequested() {
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "en")
                .body("""
                        {
                          "type": "PERSON",
                          "name": "John Doe",
                          "email": "john.locale.test@example.com",
                          "phone": "+41791234568",
                          "birthDate": "1990-05-20"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("en"))
                .header("Location", notNullValue());
    }

    @Test
    @DisplayName("Should return Content-Language: de-CH when Accept-Language: de-CH is provided")
    void shouldReturnGermanSwissLocaleWhenRequested() {
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "de-CH")
                .body("""
                        {
                          "type": "PERSON",
                          "name": "Hans Müller",
                          "email": "hans.locale.test@example.com",
                          "phone": "+41791234569",
                          "birthDate": "1988-08-10"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("de-CH"))
                .header("Location", notNullValue());
    }

    @Test
    @DisplayName("Should return Content-Language: fr-CH by default for contract operations")
    void shouldReturnFrenchSwissLocaleForContractOperations() {
        // First create a client
        final String clientId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "type": "PERSON",
                          "name": "Marie Dubois",
                          "email": "marie.contract.locale@example.com",
                          "phone": "+41791234570",
                          "birthDate": "1992-12-25"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Then create a contract and verify Content-Language
        given()
                .contentType(ContentType.JSON)
                .queryParam("clientId", clientId)
                .body("""
                        {
                          "costAmount": 1500.00
                        }
                        """)
                .when()
                .post("/v1/contracts")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH"))
                .header("Location", notNullValue());
    }

    @Test
    @DisplayName("Should return Content-Language: fr-CH for GET operations")
    void shouldReturnFrenchSwissLocaleForGetOperations() {
        // Create a client
        final String clientId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "type": "COMPANY",
                          "name": "Vaudoise Assurances",
                          "email": "info.locale@vaudoise.ch",
                          "phone": "+41219258111",
                          "companyIdentifier": "CHE-555.666.777"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Get the client and verify Content-Language
        given()
                .when()
                .get("/v1/clients/{id}", clientId)
                .then()
                .statusCode(200)
                .header("Content-Language", equalTo("fr-CH"))
                .body("id", equalTo(clientId));
    }

    @Test
    @DisplayName("Should return Content-Language: it-CH when Accept-Language: it-CH is provided")
    void shouldReturnItalianSwissLocaleWhenRequested() {
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "it-CH")
                .body("""
                        {
                          "type": "PERSON",
                          "name": "Marco Rossi",
                          "email": "marco.locale.test@example.com",
                          "phone": "+41791234571",
                          "birthDate": "1987-07-15"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("it-CH"))
                .header("Location", notNullValue());
    }

    @Test
    @DisplayName("Should fallback to fr-CH for unsupported locale")
    void shouldFallbackToFrenchSwissForUnsupportedLocale() {
        given()
                .contentType(ContentType.JSON)
                .header("Accept-Language", "zh-CN") // Chinese not supported
                .body("""
                        {
                          "type": "PERSON",
                          "name": "Test User",
                          "email": "test.unsupported.locale@example.com",
                          "phone": "+41791234572",
                          "birthDate": "1995-01-01"
                        }
                        """)
                .when()
                .post("/v1/clients")
                .then()
                .statusCode(201)
                .header("Content-Language", equalTo("fr-CH"))
                .header("Location", notNullValue());
    }
}

