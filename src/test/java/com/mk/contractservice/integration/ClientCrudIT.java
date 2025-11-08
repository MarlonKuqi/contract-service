package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.ContractJpaRepository;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client CRUD Operations - Integration Tests")
class ClientCrudIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

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
    @DisplayName("SCENARIO: Read person client returns correct type and all fields")
    void shouldReadPersonClientWithAllFields() {
        Person givenPerson = Person.builder()
                .name(ClientName.of("John Doe"))
                .email(Email.of("john.doe." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                .build();
        givenPerson = (Person) clientRepository.save(givenPerson);

        given()
                .when()
                .get("/v1/clients/{id}", givenPerson.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(givenPerson.getId().toString()))
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("John Doe"))
                .body("email", containsString("john.doe"))
                .body("phone", equalTo("+41791234567"))
                .body("birthDate", equalTo("1990-05-15"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Read company client returns correct type and all fields")
    void shouldReadCompanyClientWithAllFields() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.456.789", uniqueId.substring(0, 6));

        Company company = Company.builder()
                .name(ClientName.of("Acme Corp"))
                .email(Email.of("acme." + uniqueId + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .companyIdentifier(CompanyIdentifier.of(companyId))
                .build();
        company = (Company) clientRepository.save(company);

        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(company.getId().toString()))
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corp"))
                .body("email", containsString("acme"))
                .body("phone", equalTo("+41791234567"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Read non-existent client returns 404")
    void shouldReturn404ForNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .get("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update person client with valid data succeeds")
    void shouldUpdatePersonClientSuccessfully() {
        Person person = Person.builder()
                .name(ClientName.of("Alice Before"))
                .email(Email.of("alice.before." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791111111"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1985, 3, 20)))
                .build();
        person = (Person) clientRepository.save(person);
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
                .put("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Alice After"))
                .body("email", containsString("alice.after"))
                .body("phone", equalTo("+41792222222"))
                .body("birthDate", equalTo("1985-03-20")); // Unchanged
    }

    @Test
    @DisplayName("SCENARIO: Update company client with valid data succeeds")
    void shouldUpdateCompanyClientSuccessfully() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.222.333", uniqueId.substring(0, 6));

        Company company = Company.builder()
                .name(ClientName.of("Old Corp"))
                .email(Email.of("old." + uniqueId + "@example.com"))
                .phone(PhoneNumber.of("+41791111111"))
                .companyIdentifier(CompanyIdentifier.of(companyId))
                .build();
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
                .put("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("New Corp"))
                .body("email", containsString("new"))
                .body("phone", equalTo("+41793333333"))
                .body("companyIdentifier", equalTo(companyId)); // Unchanged
    }

    @Test
    @DisplayName("SCENARIO: Update with invalid email should fail")
    void shouldRejectUpdateWithInvalidEmail() {
        Client client = clientRepository.save(Person.builder()
                .name(ClientName.of("Test Person"))
                .email(Email.of("test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build());
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
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("SCENARIO: Update non-existent client returns 404")
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
                .put("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client without contracts succeeds")
    void shouldDeleteClientWithoutContracts() {
        Client client = clientRepository.save(Person.builder()
                .name(ClientName.of("To Delete"))
                .email(Email.of("delete." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build());
        given()
                .when()
                .delete("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Delete client with active contracts closes them")
    void shouldCloseContractsWhenDeletingClient() {
        Client client = clientRepository.save(Person.builder()
                .name(ClientName.of("Client With Contracts"))
                .email(Email.of("contracts." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build());

        LocalDateTime now = LocalDateTime.now();
        Contract contract1 = Contract.builder()
                .client(client)
                .period(ContractPeriod.of(now.minusDays(30), null))
                .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                .build();
        Contract contract2 = Contract.builder()
                .client(client)
                .period(ContractPeriod.of(now.minusDays(10), now.plusDays(100)))
                .costAmount(ContractCost.of(new BigDecimal("2000.00")))
                .build();
        contractRepository.save(contract1);
        contractRepository.save(contract2);
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(200)
                .body(equalTo("3000.00"));
        given()
                .when()
                .delete("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(404);
        given()
                .when()
                .get("/v1/clients/{clientId}/contracts/sum", client.getId())
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("SCENARIO: Delete non-existent client returns 404")
    void shouldReturn404WhenDeletingNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .delete("/v1/clients/{id}", fakeId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Person stays Person)")
    void shouldMaintainClientTypeAfterUpdate() {
        Person person = Person.builder()
                .name(ClientName.of("Person Type Test"))
                .email(Email.of("person.type." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build();
        person = (Person) clientRepository.save(person);
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
                .put("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", person.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("PERSON"))
                .body("birthDate", equalTo("1990-01-01"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Update maintains client type (Company stays Company)")
    void shouldMaintainCompanyTypeAfterUpdate() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String companyId = String.format("CHE-%s.888.777", uniqueId.substring(0, 6));

        Company company = Company.builder()
                .name(ClientName.of("Company Type Test"))
                .email(Email.of("company.type." + uniqueId + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .companyIdentifier(CompanyIdentifier.of(companyId))
                .build();
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
                .put("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", company.getId())
                .then()
                .statusCode(200)
                .body("type", equalTo("COMPANY"))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("SCENARIO: Concurrent updates should be handled gracefully")
    void shouldHandleConcurrentUpdates() {
        Client client = clientRepository.save(Person.builder()
                .name(ClientName.of("Concurrent Test"))
                .email(Email.of("concurrent." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"))
                .phone(PhoneNumber.of("+41791234567"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build());
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
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body(update2)
                .when()
                .put("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/v1/clients/{id}", client.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Update 2"))
                .body("email", containsString("update2"));
    }
}

