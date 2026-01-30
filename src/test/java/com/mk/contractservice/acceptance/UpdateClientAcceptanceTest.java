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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: Update Client")
class UpdateClientAcceptanceTest {

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
    @DisplayName("GIVEN person WHEN update with valid data THEN succeeds")
    void shouldUpdatePersonClientSuccessfully() {
        Person person = Person.of(
                ClientName.of("Alice Before"),
                ClientEmail.of("alice.before." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        person = clientRepository.save(person);
        String updatePayload = """
                {
                    "name": "Alice After",
                    "email": "alice.after.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Alice After"))
                .body("email", containsString("alice.after"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1985-03-20"));
    }

    @Test
    @DisplayName("GIVEN company WHEN update with valid data THEN succeeds")
    void shouldUpdateCompanyClientSuccessfully() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.222.333", uniqueId.substring(0, 6));

        Company company = Company.of(
                ClientName.of("Old Corp"),
                ClientEmail.of("old." + uniqueId + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);
        String updatePayload = """
                {
                    "name": "New Corp",
                    "email": "new.%s@example.com",
                    "phone": "+41793333333"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("New Corp"))
                .body("email", containsString("new"))
                .body("phone", equalTo("+41793333333"))
                .body("companyIdentifier", equalTo(companyId));
    }

    @Test
    @DisplayName("GIVEN invalid email WHEN update THEN returns 422")
    void shouldRejectUpdateWithInvalidEmail() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        String invalidPayload = """
                {
                    "name": "Test Person",
                    "email": "invalid-email-format",
                    "phone": "+41791234567"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("GIVEN non-existent client WHEN update THEN returns 404")
    void shouldReturn404WhenUpdatingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        String updatePayload = """
                {
                    "name": "Ghost Client",
                    "email": "ghost@example.com",
                    "phone": "+41791234567"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GIVEN person WHEN update THEN maintains type (Person stays Person)")
    void shouldMaintainPersonTypeAfterUpdate() {
        Person person = Person.of(
                ClientName.of("Person Type Test"),
                ClientEmail.of("person.type." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        person = clientRepository.save(person);
        String updatePayload = """
                {
                    "name": "Updated Person",
                    "email": "updated.person.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("PERSON"))
                .body("birthDate", equalTo("1990-01-01"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("GIVEN company WHEN update THEN maintains type (Company stays Company)")
    void shouldMaintainCompanyTypeAfterUpdate() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.888.777", uniqueId.substring(0, 6));

        Company company = Company.of(
                ClientName.of("Company Type Test"),
                ClientEmail.of("company.type." + uniqueId + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);
        String updatePayload = """
                {
                    "name": "Updated Company",
                    "email": "updated.company.%s@example.com",
                    "phone": "+41793333333"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("COMPANY"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("GIVEN concurrent updates WHEN both execute THEN last one wins")
    void shouldHandleConcurrentUpdates() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Concurrent Test"),
                ClientEmail.of("concurrent." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        String update1 = """
                {
                    "name": "Update 1",
                    "email": "update1.%s@example.com",
                    "phone": "+41791111111"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        String update2 = """
                {
                    "name": "Update 2",
                    "email": "update2.%s@example.com",
                    "phone": "+41792222222"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(update1)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body(update2)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Update 2"))
                .body("email", containsString("update2"));
    }

    @Test
    @DisplayName("CRITICAL: Birthdate is IMMUTABLE - Cannot be updated")
    void shouldNotUpdateBirthDateWhenUpdatingPerson() {
        // GIVEN: Person with birthdate = 1990-05-15
        Person person = Person.of(
                ClientName.of("Immutable Birth"),
                ClientEmail.of("immutable.birth." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );
        person = clientRepository.save(person);

        // WHEN: Try to update with different birthdate
        String updatePayloadWithNewBirthDate = """
                {
                    "name": "Immutable Birth Updated",
                    "email": "immutable.updated.%s@example.com",
                    "phone": "+41792222222",
                    "birthDate": "1995-12-25"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(updatePayloadWithNewBirthDate)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(204);

        // THEN: Birthdate MUST remain unchanged (1990-05-15)
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Immutable Birth Updated"))
                .body("birthDate", equalTo("1990-05-15"));
    }

    @Test
    @DisplayName("GIVEN invalid phone WHEN update THEN returns 422")
    void shouldRejectUpdateWithInvalidPhone() {
        Client client = clientRepository.save(Person.of(
                ClientName.of("Test Person"),
                ClientEmail.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        ));
        String invalidPayload = """
                {
                    "name": "Test Person",
                    "email": "test.person.%s@example.com",
                    "phone": "invalid-phone"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("CRITICAL: Company identifier is IMMUTABLE - Cannot be updated")
    void shouldNotUpdateCompanyIdentifierWhenUpdatingCompany() {
        // GIVEN: Company with identifier = "CHE-123.456.789"
        String originalIdentifier = "CHE-123.456.789";
        Company company = Company.of(
                ClientName.of("Immutable ID Corp"),
                ClientEmail.of("immutable.id." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                CompanyIdentifier.of(originalIdentifier)
        );
        company = (Company) clientRepository.save(company);

        // WHEN: Try to update with different company identifier
        String newIdentifier = "CHE-999.888.777";
        String updatePayloadWithNewIdentifier = """
                {
                    "name": "Immutable ID Corp Updated",
                    "email": "immutable.updated.%s@example.com",
                    "phone": "+41793333333",
                    "companyIdentifier": "%s"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8), newIdentifier);

        given()
                .contentType(ContentType.JSON)
                .body(updatePayloadWithNewIdentifier)
                .when()
                .put(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(204);

        // THEN: Company identifier MUST remain unchanged (CHE-123.456.789)
        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Immutable ID Corp Updated"))
                .body("companyIdentifier", equalTo(originalIdentifier));
    }
}
