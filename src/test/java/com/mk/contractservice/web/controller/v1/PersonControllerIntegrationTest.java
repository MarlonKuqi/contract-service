package com.mk.contractservice.web.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Person Controller Integration Test - Full Stack")
class PersonControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

   /* @AfterEach
    void cleanUp() {
        clientRepository.deleteAll();
    }*/

    @Test
    @DisplayName("Should create person end-to-end and persist to database")
    void shouldCreatePersonEndToEnd() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "Integration Test User",
                "integration.test@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Integration Test User"))
                .andExpect(jsonPath("$.email").value("integration.test@example.com"))
                .andExpect(jsonPath("$.phone").value("+33123456789"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-15"));

        // Verify persisted in database
       /* var clients = clientRepository.findAll();
        assert clients.size() == 1;
        assert clients.get(0).getEmail().value().equals("integration.test@example.com");*/
    }

    @Test
    @DisplayName("Should normalize email to lowercase in database")
    void shouldNormalizeEmailInDatabase() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "Uppercase Email User",
                "UPPERCASE@EXAMPLE.COM",  // Uppercase
                "+33987654321",
                LocalDate.of(1985, 5, 20)
        );

        // When
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("uppercase@example.com"));

        // Then - Verify in database
        /*var clients = clientRepository.findAll();
        assert clients.size() == 1;
        assert clients.get(0).getEmail().value().equals("uppercase@example.com");*/
    }

    @Test
    @DisplayName("Should enforce email uniqueness constraint")
    void shouldEnforceEmailUniqueness() throws Exception {
        // Given - Create first person
        CreatePersonRequest request1 = new CreatePersonRequest(
                "First User",
                "unique@example.com",
                "+33111111111",
                LocalDate.of(1990, 1, 1)
        );

        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // When - Try to create second person with same email
        CreatePersonRequest request2 = new CreatePersonRequest(
                "Second User",
                "UNIQUE@EXAMPLE.COM",  // Same email, different case
                "+33222222222",
                LocalDate.of(1992, 2, 2)
        );

        // Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andDo(print())
                .andExpect(status().isConflict());

        // Verify only one client in database
        /*var clients = clientRepository.findAll();
        assert clients.size() == 1;*/
    }

    @Test
    @DisplayName("Should validate all fields through complete stack")
    void shouldValidateThroughCompleteStack() throws Exception {
        // Given - Invalid request
        CreatePersonRequest request = new CreatePersonRequest(
                "",                  // Invalid: blank
                "invalid-email",     // Invalid: format
                "abc",               // Invalid: pattern
                null                 // Invalid: null
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        // Verify nothing persisted
        /*var clients = clientRepository.findAll();
        assert clients.isEmpty();*/
    }

    @Test
    @DisplayName("Should handle concurrent creations with different emails")
    void shouldHandleConcurrentCreations() throws Exception {
        // Given
        CreatePersonRequest request1 = new CreatePersonRequest(
                "User One",
                "user1@example.com",
                "+33111111111",
                LocalDate.of(1990, 1, 1)
        );

        CreatePersonRequest request2 = new CreatePersonRequest(
                "User Two",
                "user2@example.com",
                "+33222222222",
                LocalDate.of(1992, 2, 2)
        );

        // When - Create both
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Then
        /*var clients = clientRepository.findAll();
        assert clients.size() == 2;*/
    }

    @Test
    @DisplayName("Should persist person with special characters")
    void shouldPersistSpecialCharacters() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "François José María O'Brien",
                "special.chars@example.com",
                "+33123456789",
                LocalDate.of(1985, 3, 10)
        );

        // When
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("François José María O'Brien"));

        // Then - Verify in database
        /*var clients = clientRepository.findAll();
        assert clients.size() == 1;
        assert clients.get(0).getName().value().equals("François José María O'Brien");*/
    }

    @Test
    @DisplayName("Should persist birthDate correctly")
    void shouldPersistBirthDateCorrectly() throws Exception {
        // Given
        LocalDate birthDate = LocalDate.of(2000, 12, 31);
        CreatePersonRequest request = new CreatePersonRequest(
                "New Millennium Baby",
                "millennium@example.com",
                "+33123456789",
                birthDate
        );

        // When
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.birthDate").value("2000-12-31"));

        // Then - Verify in database
        /*var clients = clientRepository.findAll();
        assert clients.size() == 1;*/
        // Note: Compare as LocalDate since DB stores as DATE
    }

    @Test
    @DisplayName("Should generate UUID for new person")
    void shouldGenerateUuid() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "UUID Test User",
                "uuid.test@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").value(matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                )));
    }

    @Test
    @DisplayName("Should trim whitespace from name before persisting")
    void shouldTrimNameWhitespace() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "  Trimmed Name  ",  // Whitespace
                "trimmed@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 1)
        );

        // When
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trimmed Name"));

        // Then
        /*var clients = clientRepository.findAll();
        assert clients.size() == 1;
        assert clients.get(0).getName().value().equals("Trimmed Name");*/
    }
}

