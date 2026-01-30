package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Create Person Client - Acceptance Tests")
class CreatePersonAcceptanceTest {

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
    @DisplayName("SCENARIO: Create person client with all fields succeeds")
    void shouldCreatePersonClientWithAllFields() {
        String uniqueEmail = "john.doe." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "John Doe",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1990-05-15"
                }
                """, uniqueEmail, phoneNumber);

        String personId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ClientEndpoints.CLIENTS_BASE + "/"))
                .body("id", notNullValue())
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("John Doe"))
                .body("email", equalTo(uniqueEmail))
                .body("phone", equalTo(phoneNumber))
                .body("birthDate", equalTo("1990-05-15"))
                .body("companyIdentifier", nullValue())
                .extract().path("id");

        // Verify persistence
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, personId)
                .then()
                .statusCode(200)
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("John Doe"))
                .body("birthDate", equalTo("1990-05-15"));
    }

    @Test
    @DisplayName("SCENARIO: Create person with special characters in name succeeds")
    void shouldCreatePersonWithSpecialCharactersInName() {
        String uniqueEmail = "francois." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "François O'Brien-Müller",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1990-01-01"
                }
                """, uniqueEmail, phoneNumber);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("name", equalTo("François O'Brien-Müller"))
                .body("type", equalTo("PERSON"));
    }

    @Test
    @DisplayName("SCENARIO: Create person with very old birth date succeeds")
    void shouldCreatePersonWithVeryOldBirthDate() {
        String uniqueEmail = "old.person." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "Very Old Person",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1920-01-01"
                }
                """, uniqueEmail, phoneNumber);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("birthDate", equalTo("1920-01-01"))
                .body("type", equalTo("PERSON"));
    }

    @Test
    @DisplayName("SCENARIO: Create person with invalid email fails")
    void shouldRejectPersonWithInvalidEmail() {
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "Invalid Email Person",
                    "email": "invalid-email-format",
                    "phone": "%s",
                    "birthDate": "1990-01-01"
                }
                """, phoneNumber);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create person with duplicate email fails")
    void shouldRejectPersonWithDuplicateEmail() {
        String duplicateEmail = "duplicate." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber1 = TestDataHelper.randomSwissPhoneNumber();
        String phoneNumber2 = TestDataHelper.randomSwissPhoneNumber();

        // Create first person
        String createPayload1 = String.format("""
                {
                    "type": "PERSON",
                    "name": "First Person",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1990-01-01"
                }
                """, duplicateEmail, phoneNumber1);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload1)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        // Try to create second person with same email
        String createPayload2 = String.format("""
                {
                    "type": "PERSON",
                    "name": "Second Person",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1985-01-01"
                }
                """, duplicateEmail, phoneNumber2);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload2)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("SCENARIO: Create person with duplicate phone fails")
    void shouldRejectPersonWithDuplicatePhone() {
        String duplicatePhone = TestDataHelper.randomSwissPhoneNumber();
        String email1 = "person1." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String email2 = "person2." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // Create first person
        String createPayload1 = String.format("""
                {
                    "type": "PERSON",
                    "name": "First Person",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1990-01-01"
                }
                """, email1, duplicatePhone);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload1)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        // Try to create second person with same phone
        String createPayload2 = String.format("""
                {
                    "type": "PERSON",
                    "name": "Second Person",
                    "email": "%s",
                    "phone": "%s",
                    "birthDate": "1985-01-01"
                }
                """, email2, duplicatePhone);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload2)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("SCENARIO: Create person without birthDate fails")
    void shouldRejectPersonWithoutBirthDate() {
        String email = "nobirth." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "PERSON",
                    "name": "No Birth Date",
                    "email": "%s",
                    "phone": "%s"
                }
                """, email, phoneNumber);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(422);
    }
}
