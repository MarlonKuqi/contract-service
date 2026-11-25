package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.MutableInstantSource;
import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import io.restassured.response.ValidatableResponse;
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

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PersonBirthDateAcceptanceTest.MutableInstantSourceConfiguration.class})
@DisplayName("Feature: Birth date of a person")
class PersonBirthDateAcceptanceTest {

    private static final Instant TODAY_IN_THE_APPLICATION = Instant.parse("2040-06-01T10:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableInstantSourceConfiguration {

        @Bean
        @Primary
        MutableInstantSource mutableInstantSource() {
            return new MutableInstantSource(TODAY_IN_THE_APPLICATION);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MutableInstantSource instantSource;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        instantSource.setInstant(TODAY_IN_THE_APPLICATION);
    }

    @Test
    @DisplayName("GIVEN a birth date before the application date WHEN creating a person THEN it is accepted, whatever the date of the server")
    void shouldCheckBirthDateAgainstTheApplicationClock() {
        createPersonBornOn("2040-05-31").statusCode(201);
    }

    @Test
    @DisplayName("GIVEN a birth date after the application date WHEN creating a person THEN 422 with the code of the domain")
    void shouldRejectBirthDateInTheFuture() {
        createPersonBornOn("2040-06-02")
                .statusCode(422)
                .body("code", equalTo("BIRTH_DATE_INVALID"));
    }

    @Test
    @DisplayName("GIVEN a birth date that is not a date WHEN creating a person THEN 422 naming the field")
    void shouldNameTheFieldWhenBirthDateIsNotADate() {
        createPersonBornOn("not-a-date")
                .statusCode(422)
                .body("code", equalTo("validationError"))
                .body("detail", equalTo("Invalid value 'not-a-date' for field 'birthDate'. Expected type: LocalDate"));
    }

    private ValidatableResponse createPersonBornOn(final String birthDate) {
        return given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Alice Martin",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "%s"
                        }
                        """.formatted(
                        TestDataHelper.uniqueEmail("birth.date"),
                        TestDataHelper.randomSwissPhoneNumber(),
                        birthDate))
                .post(ClientEndpoints.CLIENTS_BASE)
                .then();
    }
}
