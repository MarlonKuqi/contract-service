package com.mk.contractservice.infrastructure.web.client;

import com.mk.contractservice.application.feature.client.create.CreateCompany;
import com.mk.contractservice.application.feature.client.create.CreatePerson;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.infrastructure.web.client.shared.ClientDtoMapper;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.web.client.shared.PersonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreateClientController.class)
@ActiveProfiles("test")
@DisplayName("CreateClientController - Tests WebMvc")
class CreateClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePerson createPerson;

    @MockitoBean
    private CreateCompany createCompany;

    @MockitoBean
    private ClientDtoMapper clientDtoMapper;

    @Nested
    @DisplayName("POST /api/v2/clients - Création de Person")
    class CreatePersonTests {

        @Test
        @DisplayName("GIVEN requête Person valide WHEN POST THEN retourne 201 avec Location header")
        void shouldCreatePersonSuccessfully() throws Exception {
            // Given
            UUID personId = UUID.randomUUID();
            String name = "Alice Martin";
            String email = "alice.martin@example.com";
            String phone = "+41791234567";
            LocalDate birthDate = LocalDate.of(1990, 5, 15);

            Person person = Person.builder()
                    .id(personId)
                    .name(ClientName.of(name))
                    .email(ClientEmail.of(email))
                    .phone(ClientPhoneNumber.of(phone))
                    .birthDate(PersonBirthDate.of(birthDate))
                    .build();

            when(createPerson.execute(any(CreatePerson.Command.class))).thenReturn(person);
            when(clientDtoMapper.toResponse(person)).thenReturn(
                    new PersonResponse(
                            personId,
                            name,
                            email.toLowerCase(),
                            phone,
                            birthDate
                    )
            );

            String requestBody = """
                    {
                        "type": "PERSON",
                        "name": "Alice Martin",
                        "email": "alice.martin@example.com",
                        "phone": "+41791234567",
                        "birthDate": "1990-05-15"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/v2/clients/" + personId))
                    .andExpect(jsonPath("$.id").value(personId.toString()))
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                    .andExpect(jsonPath("$.phone").value(phone))
                    .andExpect(jsonPath("$.birthDate").value("1990-05-15"));
        }

        @Test
        @DisplayName("GIVEN email en majuscules WHEN POST THEN normalise en minuscules")
        void shouldNormalizeEmailToLowercase() throws Exception {
            // Given
            UUID personId = UUID.randomUUID();
            String email = "Alice.Martin@Example.COM";

            Person person = Person.builder()
                    .id(personId)
                    .name(ClientName.of("Alice Martin"))
                    .email(ClientEmail.of(email))
                    .phone(ClientPhoneNumber.of("+41791234567"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                    .build();

            when(createPerson.execute(any(CreatePerson.Command.class))).thenReturn(person);
            when(clientDtoMapper.toResponse(person)).thenReturn(
                    new PersonResponse(
                            personId,
                            "Alice Martin",
                            "alice.martin@example.com",
                            "+41791234567",
                            LocalDate.of(1990, 5, 15)
                    )
            );

            String requestBody = """
                    {
                        "type": "PERSON",
                        "name": "Alice Martin",
                        "email": "Alice.Martin@Example.COM",
                        "phone": "+41791234567",
                        "birthDate": "1990-05-15"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("alice.martin@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/v2/clients - Création de Company")
    class CreateCompanyTests {

        @Test
        @DisplayName("GIVEN requête Company valide WHEN POST THEN retourne 201 avec Location header")
        void shouldCreateCompanySuccessfully() throws Exception {
            // Given
            UUID companyId = UUID.randomUUID();
            String name = "Acme Corporation";
            String email = "contact@acme.com";
            String phone = "+41221234567";
            String companyIdentifier = "CHE-123.456.789";

            Company company = Company.builder()
                    .id(companyId)
                    .name(ClientName.of(name))
                    .email(ClientEmail.of(email))
                    .phone(ClientPhoneNumber.of(phone))
                    .companyIdentifier(CompanyIdentifier.of(companyIdentifier))
                    .build();

            when(createCompany.execute(any(CreateCompany.Command.class))).thenReturn(company);
            when(clientDtoMapper.toResponse(company)).thenReturn(
                    new com.mk.contractservice.infrastructure.web.client.shared.CompanyResponse(
                            companyId,
                            name,
                            email.toLowerCase(),
                            phone,
                            companyIdentifier
                    )
            );

            String requestBody = """
                    {
                        "type": "COMPANY",
                        "name": "Acme Corporation",
                        "email": "contact@acme.com",
                        "phone": "+41221234567",
                        "companyIdentifier": "CHE-123.456.789"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/v2/clients/" + companyId))
                    .andExpect(jsonPath("$.id").value(companyId.toString()))
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                    .andExpect(jsonPath("$.phone").value(phone))
                    .andExpect(jsonPath("$.companyIdentifier").value(companyIdentifier));
        }
    }

    @Nested
    @DisplayName("POST /api/v2/clients - Validation des erreurs Person")
    class PersonValidationErrorTests {

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                GIVEN name manquant WHEN POST THEN retourne 422 | {"type":"PERSON","email":"alice.martin@example.com","phone":"+41791234567","birthDate":"1990-05-15"}
                GIVEN email manquant WHEN POST THEN retourne 422 | {"type":"PERSON","name":"Alice Martin","phone":"+41791234567","birthDate":"1990-05-15"}
                GIVEN birthDate manquante WHEN POST THEN retourne 422 | {"type":"PERSON","name":"Alice Martin","email":"alice.martin@example.com","phone":"+41791234567"}
                GIVEN email invalide WHEN POST THEN retourne 422 | {"type":"PERSON","name":"Alice Martin","email":"invalid-email","phone":"+41791234567","birthDate":"1990-05-15"}
                GIVEN birthDate future WHEN POST THEN retourne 422 | {"type":"PERSON","name":"Alice Martin","email":"alice.martin@example.com","phone":"+41791234567","birthDate":"2099-12-31"}
                """)
        void shouldRejectInvalidPersonData(String displayName, String requestBody) throws Exception {
            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("GIVEN email déjà existant WHEN POST THEN retourne 409")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Given
            String requestBody = """
                    {
                        "type": "PERSON",
                        "name": "Alice Martin",
                        "email": "alice.martin@example.com",
                        "phone": "+41791234567",
                        "birthDate": "1990-05-15"
                    }
                    """;

            when(createPerson.execute(any(CreatePerson.Command.class)))
                    .thenThrow(new ClientAlreadyExistsException("Email already exists"));

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/clients - Validation des erreurs Company")
    class CompanyValidationErrorTests {

        @Test
        @DisplayName("GIVEN companyIdentifier manquant WHEN POST THEN retourne 422")
        void shouldRejectMissingCompanyIdentifier() throws Exception {
            // Given
            String requestBody = """
                    {
                        "type": "COMPANY",
                        "name": "Acme Corporation",
                        "email": "contact@acme.com",
                        "phone": "+41221234567"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent()); // 422
        }

        @Test
        @DisplayName("GIVEN type manquant WHEN POST THEN retourne 422")
        void shouldRejectMissingType() throws Exception {
            // Given
            String requestBody = """
                    {
                        "name": "Acme Corporation",
                        "email": "contact@acme.com",
                        "phone": "+41221234567",
                        "companyIdentifier": "CHE-123.456.789"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("GIVEN companyIdentifier déjà existant WHEN POST THEN retourne 409")
        void shouldReturn409WhenCompanyIdentifierAlreadyExists() throws Exception {
            // Given
            String requestBody = """
                    {
                        "type": "COMPANY",
                        "name": "Acme Corporation",
                        "email": "contact@acme.com",
                        "phone": "+41221234567",
                        "companyIdentifier": "CHE-123.456.789"
                    }
                    """;

            when(createCompany.execute(any(CreateCompany.Command.class)))
                    .thenThrow(new ClientAlreadyExistsException("Company identifier already exists"));

            // When & Then
            mockMvc.perform(post(ClientEndpoints.CLIENTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isConflict());
        }
    }
}

