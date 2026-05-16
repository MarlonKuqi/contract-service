package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
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
@DisplayName("Feature: Read Client")
class ReadClientAcceptanceTest {

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
    @DisplayName("GIVEN person exists WHEN get by id THEN returns person with all fields")
    void shouldReadPersonClientWithAllFields() {
        final String phoneNumber = TestDataHelper.randomSwissPhoneNumber();
        Person givenPerson = Person.of(
                ClientName.of("John Doe"),
                ClientEmail.of(TestDataHelper.uniqueEmail("john.doe")),
                ClientPhoneNumber.of(phoneNumber),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );
        givenPerson = (Person) clientRepository.save(givenPerson);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, givenPerson.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(givenPerson.getId().toString()))
                .body("type", equalTo("PERSON"))
                .body("name", equalTo("John Doe"))
                .body("email", containsString("john.doe"))
                .body("phone", equalTo(phoneNumber))
                .body("birthDate", equalTo("1990-05-15"))
                .body("companyIdentifier", nullValue());
    }

    @Test
    @DisplayName("GIVEN company exists WHEN get by id THEN returns company with all fields")
    void shouldReadCompanyClientWithAllFields() {
        String companyId = TestDataHelper.uniqueCompanyIdentifier("CHE");
        String phoneNumber = TestDataHelper.randomSwissPhoneNumber();

        Company company = Company.of(
                ClientName.of("Acme Corp"),
                ClientEmail.of(TestDataHelper.uniqueEmail("acme")),
                ClientPhoneNumber.of(phoneNumber),
                CompanyIdentifier.of(companyId)
        );
        company = (Company) clientRepository.save(company);

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, company.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(company.getId().toString()))
                .body("type", equalTo("COMPANY"))
                .body("name", equalTo("Acme Corp"))
                .body("email", containsString("acme"))
                .body("phone", equalTo(phoneNumber))
                .body("companyIdentifier", equalTo(companyId))
                .body("birthDate", nullValue());
    }

    @Test
    @DisplayName("GIVEN non-existent client WHEN get by id THEN returns 404")
    void shouldReturn404ForNonExistentClient() {
        UUID fakeId = UUID.randomUUID();

        given()
                .when()
                .get(ClientEndpoints.CLIENT_BY_ID, fakeId)
                .then()
                .statusCode(404);
    }
}
