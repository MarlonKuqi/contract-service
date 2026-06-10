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
@DisplayName("Create Company Client - Acceptance Tests")
class CreateCompanyAcceptanceTest {

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
    @DisplayName("SCENARIO: Create company client with all fields succeeds")
    void shouldCreateCompanyClientWithAllFields() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyIdentifier = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String uniqueEmail = "acme." + uniqueId + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Acme Corporation",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, uniqueEmail, phoneNumber, companyIdentifier);

        String companyId = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .header("Location", containsString(ClientEndpoints.CLIENTS_BASE + "/"))
                .body("id", notNullValue())
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corporation"))
                .body("email", equalTo(uniqueEmail))
                .body("phone", equalTo(phoneNumber))
                .body("companyIdentifier", equalTo(companyIdentifier))
                .body("birthDate", nullValue())
                .extract().path("id");

        // Verify persistence
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, companyId)
                .then()
                .statusCode(200)
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corporation"))
                .body("companyIdentifier", equalTo(companyIdentifier));
    }

    @Test
    @DisplayName("SCENARIO: Create company with special characters in name succeeds")
    void shouldCreateCompanyWithSpecialCharactersInName() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyIdentifier = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String uniqueEmail = "special." + uniqueId + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Société Française & Cie SA",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, uniqueEmail, phoneNumber, companyIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .body("name", equalTo("Société Française & Cie SA"))
                .body("type", equalTo("COMPANY"));
    }

    @Test
    @DisplayName("SCENARIO: Create company with invalid email fails")
    void shouldRejectCompanyWithInvalidEmail() {
        String companyIdentifier = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Invalid Email Corp",
                    "email": "invalid-email-format",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, phoneNumber, companyIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Create company with duplicate email fails")
    void shouldRejectCompanyWithDuplicateEmail() {
        String duplicateEmail = "duplicate." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String companyId1 = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String companyId2 = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String phone1 = TestDataHelper.randomSwissPhoneNumber();
        String phone2 = TestDataHelper.randomSwissPhoneNumber();

        // Create first company
        String createPayload1 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "First Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, duplicateEmail, phone1, companyId1);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload1)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        // Try to create second company with same email
        String createPayload2 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Second Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, duplicateEmail, phone2, companyId2);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload2)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("SCENARIO: Create company with duplicate phone fails")
    void shouldRejectCompanyWithDuplicatePhone() {
        String duplicatePhone = TestDataHelper.randomSwissPhoneNumber();
        String companyId1 = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String companyId2 = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String email1 = "company1." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String email2 = "company2." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // Create first company
        String createPayload1 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "First Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, email1, duplicatePhone, companyId1);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload1)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        // Try to create second company with same phone
        String createPayload2 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Second Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, email2, duplicatePhone, companyId2);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload2)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("SCENARIO: Create company with duplicate identifier fails")
    void shouldRejectCompanyWithDuplicateIdentifier() {
        String duplicateIdentifier = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String email1 = "company1." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String email2 = "company2." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phone1 = TestDataHelper.randomSwissPhoneNumber();
        String phone2 = TestDataHelper.randomSwissPhoneNumber();

        // Create first company
        String createPayload1 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "First Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, email1, phone1, duplicateIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload1)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201);

        // Try to create second company with same identifier
        String createPayload2 = String.format("""
                {
                    "type": "COMPANY",
                    "name": "Second Company",
                    "email": "%s",
                    "phone": "%s",
                    "companyIdentifier": "%s"
                }
                """, email2, phone2, duplicateIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(createPayload2)
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("SCENARIO: Create company without companyIdentifier fails")
    void shouldRejectCompanyWithoutIdentifier() {
        String email = "noidentifier." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        String createPayload = String.format("""
                {
                    "type": "COMPANY",
                    "name": "No Identifier Company",
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

    @Test
    @DisplayName("SCENARIO: Create company with various international phone formats succeeds")
    void shouldCreateCompanyWithInternationalPhones() {
        String[] validPhones = {
                "+41 79 123 45 67",    // Switzerland
                "+33 6 12 34 56 78",   // France
                "+39 345 123 4567",    // Italy
                "+49 151 12345678",    // Germany
                "+41211234567"         // Switzerland (no spaces)
        };

        for (String phone : validPhones) {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String companyIdentifier = TestDataHelper.uniqueCompanyIdentifier("CHE");
            String email = "intl." + uniqueId + "@example.com";

            String createPayload = String.format("""
                    {
                        "type": "COMPANY",
                        "name": "International Corp",
                        "email": "%s",
                        "phone": "%s",
                        "companyIdentifier": "%s"
                    }
                    """, email, phone, companyIdentifier);

            given()
                    .contentType(ContentType.JSON)
                    .body(createPayload)
                    .when()
                    .post(ClientEndpoints.CLIENTS_BASE)
                    .then()
                    .statusCode(201)
                    .body("type", equalTo("COMPANY"));
        }
    }
}
