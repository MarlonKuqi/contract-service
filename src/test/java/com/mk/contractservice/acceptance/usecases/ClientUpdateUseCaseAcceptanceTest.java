package com.mk.contractservice.acceptance.usecases;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("UC03: Update Client Use Case - Integration Tests")
class ClientUpdateUseCaseAcceptanceTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Nested
    @DisplayName("Update Person Client")
    class UpdatePerson {

        @Test
        @DisplayName("Should successfully update name, email, and phone for Person")
        void shouldUpdatePersonCommonFields() {
            // GIVEN: A Person client
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String createPayload = """
                    {
                        "type": "PERSON",
                        "name": "Original Name",
                        "email": "original.%s@example.com",
                        "phone": "+41795000001",
                        "birthDate": "1988-04-20"
                    }
                    """.formatted(uniqueId);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // WHEN: Update name, email, phone
            String updatePayload = """
                    {
                        "type": "PERSON",
                        "name": "Updated Name",
                        "email": "updated.%s@example.com",
                        "phone": "+41792222222"
                    }
                    """.formatted(UUID.randomUUID().toString().substring(0, 8));

            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayload)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204);

            // THEN: Name, email, phone are updated
            given()
                    .when()
                    .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Updated Name"))
                    .body("email", containsString("updated"))
                    .body("phone", equalTo("+41792222222"));
        }

        @Test
        @DisplayName("CRITICAL: Birthdate is IMMUTABLE - Cannot be updated (requirement from sujet.txt)")
        void shouldNotUpdateBirthDateWhenUpdatingPerson() {
            // GIVEN
            String uniqueId1 = UUID.randomUUID().toString().substring(0, 8);
            String uniqueId2 = UUID.randomUUID().toString().substring(0, 8);
            String phone1 = TestDataHelper.randomSwissPhoneNumber();
            String phone2 = TestDataHelper.randomSwissPhoneNumber();

            String createPayload = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Immutable Birth",
                        "email": "immutable.birth.%s@example.com",
                        "phone": "%s",
                        "birthDate": "1990-05-15"
                    }
                    """, uniqueId1, phone1);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .body("birthDate", equalTo("1990-05-15"))
                    .extract().path("id");

            // WHEN: Try to update with different birthdate
            String updatePayloadWithNewBirthDate = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Immutable Birth Updated",
                        "email": "immutable.updated.%s@example.com",
                        "phone": "%s",
                        "birthDate": "1995-12-25"
                    }
                    """, uniqueId2, phone2);

            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayloadWithNewBirthDate)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(anyOf(is(204), is(400), is(422)));  // Success or validation error

            // THEN: Birthdate MUST remain unchanged (1990-05-15)
            given()
                    .when()
                    .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(200)
                    .body("name", anyOf(equalTo("Immutable Birth Updated"), equalTo("Immutable Birth")))
                    .body("birthDate", equalTo("1990-05-15"));  // ← CRITICAL: Birthdate MUST NOT change
        }

        @Test
        @DisplayName("Should reject update with invalid email format")
        void shouldRejectInvalidEmailWhenUpdatingPerson() {
            // GIVEN: A Person client
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String phone = TestDataHelper.randomSwissPhoneNumber();

            String createPayload = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Test Person",
                        "email": "test.person.%s@example.com",
                        "phone": "%s",
                        "birthDate": "1990-01-01"
                    }
                    """, uniqueId, phone);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // WHEN: Try to update with invalid email
            String updatePayloadInvalidEmail = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Test Person",
                        "email": "invalid-email-format",
                        "phone": "%s"
                    }
                    """, phone);

            // THEN: Should reject with validation error
            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayloadInvalidEmail)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(anyOf(is(400), is(422)));
        }

        @Test
        @DisplayName("Should reject update with invalid phone format")
        void shouldRejectInvalidPhoneWhenUpdatingPerson() {
            // GIVEN: A Person client
            String phone = TestDataHelper.randomSwissPhoneNumber();
            String createPayload = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Test Person",
                        "email": "test.person.%s@example.com",
                        "phone": "%s",
                        "birthDate": "1990-01-01"
                    }
                    """, UUID.randomUUID().toString().substring(0, 8), phone);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // WHEN: Try to update with invalid phone
            String updatePayloadInvalidPhone = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Test Person",
                        "email": "test.person.%s@example.com",
                        "phone": "invalid-phone"
                    }
                    """, UUID.randomUUID().toString().substring(0, 8));

            // THEN: Should reject with validation error
            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayloadInvalidPhone)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(anyOf(is(400), is(422)));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent Person")
        void shouldReturn404WhenUpdatingNonExistentPerson() {
            // GIVEN: A non-existent client ID
            UUID fakeClientId = UUID.randomUUID();

            // WHEN: Try to update
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String updatePayload = String.format("""
                    {
                        "type": "PERSON",
                        "name": "Test Person",
                        "email": "test.%s@example.com",
                        "phone": "%s"
                    }
                    """, uniqueId, TestDataHelper.randomSwissPhoneNumber());

            // THEN: Should return 404
            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayload)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, fakeClientId)
                    .then()
                    .statusCode(404);
        }
    }

    // ========================================
    // UPDATE COMPANY
    // ========================================

    @Nested
    @DisplayName("Update Company Client")
    class UpdateCompany {

        @Test
        @DisplayName("Should successfully update name, email, and phone for Company")
        void shouldUpdateCompanyCommonFields() {
            // GIVEN: A Company client
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String companyIdPart = uniqueId.substring(0, 6);
            String phone1 = TestDataHelper.randomSwissPhoneNumber();
            String phone2 = TestDataHelper.randomSwissPhoneNumber();

            String createPayload = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "Original Tech SA",
                        "email": "original.tech.%s@example.com",
                        "phone": "%s",
                        "companyIdentifier": "CHE-%s.222.333"
                    }
                    """, uniqueId, phone1, companyIdPart);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            String originalIdentifier = String.format("CHE-%s.222.333", companyIdPart);

            // WHEN: Update name, email, phone
            String updateUniqueId = UUID.randomUUID().toString().substring(0, 8);
            String updatePayload = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "Updated Tech SA",
                        "email": "updated.tech.%s@example.com",
                        "phone": "%s"
                    }
                    """, updateUniqueId, phone2);

            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayload)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(204);

            // THEN: Name, email, phone are updated, but companyIdentifier remains unchanged
            given()
                    .when()
                    .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Updated Tech SA"))
                    .body("email", containsString("updated.tech"))
                    .body("phone", equalTo(phone2))
                    .body("companyIdentifier", equalTo(originalIdentifier));
        }

        @Test
        @DisplayName("CRITICAL: Company identifier is IMMUTABLE - Cannot be updated (requirement from sujet.txt)")
        void shouldNotUpdateCompanyIdentifierWhenUpdatingCompany() {
            // ========================================
            // GIVEN: Company with identifier = "CHE-123.456.789"
            // ========================================
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String originalIdentifier = "CHE-123.456.789";
            String phoneNumber1 = TestDataHelper.randomSwissPhoneNumber();
            String phoneNumber2 = TestDataHelper.randomSwissPhoneNumber();

            String createPayload = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "Immutable ID Corp",
                        "email": "immutable.id.%s@example.com",
                        "phone": "%s",
                        "companyIdentifier": "%s"
                    }
                    """, uniqueId, phoneNumber1, originalIdentifier);

            String clientId = given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .body("companyIdentifier", equalTo(originalIdentifier))
                    .extract().path("id");

            // ========================================
            // WHEN: Try to update with different company identifier
            // ========================================
            String updateUniqueId = UUID.randomUUID().toString().substring(0, 8);
            String newIdentifier = "CHE-999.888.777";

            String updatePayloadWithNewIdentifier = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "Immutable ID Corp Updated",
                        "email": "immutable.updated.%s@example.com",
                        "phone": "%s",
                        "companyIdentifier": "%s"
                    }
                    """, updateUniqueId, phoneNumber2, newIdentifier);

            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayloadWithNewIdentifier)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(anyOf(is(204), is(400), is(422)));  // Success or validation error

            // THEN: Company identifier MUST remain unchanged (CHE-123.456.789)

            given()
                    .when()
                    .get(ClientEndpoints.CLIENT_BY_ID, clientId)
                    .then()
                    .statusCode(200)
                    .body("name", anyOf(equalTo("Immutable ID Corp Updated"), equalTo("Immutable ID Corp")))
                    .body("companyIdentifier", equalTo(originalIdentifier));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent Company")
        void shouldReturn404WhenUpdatingNonExistentCompany() {
            // GIVEN: A non-existent client ID
            UUID fakeClientId = UUID.randomUUID();

            // WHEN: Try to update
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String updatePayload = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "Test Company",
                        "email": "test.%s@example.com",
                        "phone": "%s"
                    }
                    """, uniqueId, TestDataHelper.randomSwissPhoneNumber());

            // THEN: Should return 404
            given()
                    .contentType(ContentType.JSON)
                    .body(updatePayload)
                    .when()
                    .put(ClientEndpoints.CLIENT_BY_ID, fakeClientId)
                    .then()
                    .statusCode(404);
        }
    }
}

