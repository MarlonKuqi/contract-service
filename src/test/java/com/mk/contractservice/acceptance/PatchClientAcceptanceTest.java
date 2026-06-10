package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyIdentifier;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: Patch Client")
class PatchClientAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        clientJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("GIVEN person WHEN patch with only name THEN updates only name")
    void shouldPatchPersonClientWithOnlyName() {
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();
        Person person = Person.of(
                ClientName.of("Original Name"),
                ClientEmail.of("original." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(phoneNumber),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "name": "Patched Name"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Patched Name"))
                .body("email", containsString("original"))
                .body("phone", equalTo(phoneNumber))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("GIVEN person WHEN patch with only email THEN updates only email")
    void shouldPatchPersonClientWithOnlyEmail() {
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("old." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(phoneNumber),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "email": "new.%s@example.com"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("new"))
                .body("phone", equalTo(phoneNumber))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("GIVEN person WHEN patch with only phone THEN updates only phone")
    void shouldPatchPersonClientWithOnlyPhone() {
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String patchPayload = """
                {
                    "phone": "+41799999999"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("test"))
                .body("phone", equalTo("+41799999999"))
                .body("birthDate", equalTo("1990-01-01"));
    }

    @Test
    @DisplayName("GIVEN company WHEN patch with multiple fields THEN updates only those fields")
    void shouldPatchCompanyClientWithMultipleFields() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.999.888", uniqueId.substring(0, 6));
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        Company company = Company.of(
                ClientName.of("Old Corp"),
                ClientEmail.of("old." + uniqueId + "@example.com"),
                ClientPhoneNumber.of(phoneNumber),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        String patchPayload = """
                {
                    "name": "Patched Corp",
                    "email": "patched.%s@example.com"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Patched Corp"))
                .body("email", containsString("patched"))
                .body("phone", equalTo(phoneNumber))
                .body("companyIdentifier", equalTo(companyId));
    }

    @Test
    @DisplayName("GIVEN invalid email WHEN patch THEN returns 400/422")
    void shouldRejectPatchWithInvalidEmail() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        String invalidPatchPayload = """
                {
                    "email": "invalid-email"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("GIVEN invalid phone WHEN patch THEN returns 400/422")
    void shouldRejectPatchWithInvalidPhone() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));

        String invalidPatchPayload = """
                {
                    "phone": "invalid"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("GIVEN non-existent client WHEN patch THEN returns 404")
    void shouldReturn404WhenPatchingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        String patchPayload = """
                {
                    "name": "Ghost Name"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GIVEN empty body WHEN patch THEN succeeds with no changes")
    void shouldAcceptPatchWithEmptyBody() {
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();
        Person person = Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(phoneNumber),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = (Person) clientRepository.save(person);

        String emptyPatchPayload = "{}";

        given()
                .contentType(ContentType.JSON)
                .body(emptyPatchPayload)
                .when()
                .patch(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Person"))
                .body("email", containsString("test"))
                .body("phone", equalTo(phoneNumber));
    }
}
