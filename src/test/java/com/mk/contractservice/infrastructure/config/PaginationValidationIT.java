package com.mk.contractservice.infrastructure.config;

import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Pagination Validation Integration Tests")
class PaginationValidationIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("Should accept valid pagination parameters")
    void shouldAcceptValidPagination() {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", 0)
                .param("size", 20)
                .when()
                .get("/v2/contracts")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 20, 'Page number must not be less than zero'",
            "-10, 20, 'Page number must not be less than zero'",
            "-100, 20, 'Page number must not be less than zero'"
    })
    @DisplayName("Should reject negative page numbers")
    void shouldRejectNegativePageNumber(int page, int size, String expectedMessage) {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", page)
                .param("size", size)
                .when()
                .get("/v2/contracts")
                .then()
                .log().all() // Log la réponse complète pour debug
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("title", containsString("Invalid Parameter"))
                .body("detail", containsString(expectedMessage));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 'Page size must not be less than one'",
            "0, -1, 'Page size must not be less than one'",
            "0, -10, 'Page size must not be less than one'"
    })
    @DisplayName("Should reject page size less than 1")
    void shouldRejectPageSizeLessThanOne(int page, int size, String expectedMessage) {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", page)
                .param("size", size)
                .when()
                .get("/v2/contracts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("title", containsString("Invalid Parameter"))
                .body("detail", containsString(expectedMessage));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 101, 'Page size must not exceed 100'",
            "0, 150, 'Page size must not exceed 100'",
            "0, 200, 'Page size must not exceed 100'"
    })
    @DisplayName("Should reject page size greater than max")
    void shouldRejectPageSizeGreaterThanMax(int page, int size, String expectedMessage) {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", page)
                .param("size", size)
                .when()
                .get("/v2/contracts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("title", containsString("Invalid Parameter"))
                .body("detail", containsString(expectedMessage));
    }

    @Test
    @DisplayName("Should accept page size equal to max (100)")
    void shouldAcceptPageSizeEqualToMax() {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", 0)
                .param("size", 100)
                .when()
                .get("/v2/contracts")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @CsvSource({
            "abc, 20, 'Invalid page number format'",
            "0, xyz, 'Invalid page size format'"
    })
    @DisplayName("Should reject invalid number formats")
    void shouldRejectInvalidFormats(String page, String size, String expectedMessage) {
        UUID clientId = UUID.randomUUID();

        given()
                .param("clientId", clientId.toString())
                .param("page", page)
                .param("size", size)
                .when()
                .get("/v2/contracts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("title", containsString("Invalid Parameter"))
                .body("detail", containsString(expectedMessage));
    }
}

