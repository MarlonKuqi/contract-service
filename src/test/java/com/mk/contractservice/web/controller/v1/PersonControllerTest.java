package com.mk.contractservice.web.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonController.class)
@DisplayName("PersonController - POST /v1/clients/persons")
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientApplicationService clientApplicationService;

    @Test
    @DisplayName("Should create person and return 201 Created with Location header")
    void shouldCreatePersonSuccessfully() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        UUID personId = UUID.randomUUID();
        Person savedPerson = new Person(
                ClientName.of("John Doe"),
                Email.of("john.doe@example.com"),
                PhoneNumber.of("+33123456789"),
                LocalDate.of(1990, 1, 15)
        );

        when(clientApplicationService.createPerson(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(savedPerson);

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/v1/clients/" + personId)))
                .andExpect(jsonPath("$.id").value(personId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+33123456789"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-15"));

        verify(clientApplicationService).createPerson(
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("+33123456789"),
                eq(LocalDate.of(1990, 1, 15))
        );
    }

    @Test
    @DisplayName("Should return 400 Bad Request when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when email is invalid")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "invalid-email",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when phone pattern is invalid")
    void shouldReturn400WhenPhoneIsInvalid() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "abc",
                LocalDate.of(1990, 1, 15)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when birthDate is null")
    void shouldReturn400WhenBirthDateIsNull() throws Exception {
        // Given
        String requestJson = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "phone": "+33123456789"
                }
                """;

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when multiple fields are invalid")
    void shouldReturn400WhenMultipleFieldsInvalid() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "",
                "invalid-email",
                "abc",
                null
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 409 Conflict when email already exists")
    void shouldReturn409WhenEmailExists() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        when(clientApplicationService.createPerson(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDate.class)
        )).thenThrow(new ClientAlreadyExistsException(
                "Client already exists",
                "john.doe@example.com"
        ));

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());

        verify(clientApplicationService).createPerson(
                eq("John Doe"),
                eq("john.doe@example.com"),
                eq("+33123456789"),
                eq(LocalDate.of(1990, 1, 15))
        );
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldAcceptSpecialCharactersInName() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "Jean-François O'Connor",
                "jf@example.com",
                "+33123456789",
                LocalDate.of(1985, 3, 10)
        );

        UUID personId = UUID.randomUUID();
        Person savedPerson = new Person(
                ClientName.of("Jean-François O'Connor"),
                Email.of("jf@example.com"),
                PhoneNumber.of("+33123456789"),
                LocalDate.of(1985, 3, 10)
        );

        when(clientApplicationService.createPerson(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(savedPerson);

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jean-François O'Connor"));
    }

    @Test
    @DisplayName("Should accept various valid phone formats")
    void shouldAcceptVariousPhoneFormats() throws Exception {
        // Given
        String[] validPhones = {
                "+33123456789",
                "+33 1 23 45 67 89",
                "+33-1-23-45-67-89",
                "+33.1.23.45.67.89",
                "0123456789"
        };

        for (String phone : validPhones) {
            CreatePersonRequest request = new CreatePersonRequest(
                    "Test User",
                    "test" + System.nanoTime() + "@example.com",
                    phone,
                    LocalDate.of(1990, 1, 1)
            );

            UUID personId = UUID.randomUUID();
            Person savedPerson = new Person(
                    ClientName.of("Test User"),
                    Email.of(request.email()),
                    PhoneNumber.of(phone),
                    LocalDate.of(1990, 1, 1)
            );

            when(clientApplicationService.createPerson(
                    anyString(),
                    anyString(),
                    anyString(),
                    any(LocalDate.class)
            )).thenReturn(savedPerson);

            // When / Then
            mockMvc.perform(post("/v1/clients/persons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.phone").value(phone));

            reset(clientApplicationService);
        }
    }

    @Test
    @DisplayName("Should return 400 when Content-Type is not application/json")
    void shouldReturn400WhenContentTypeIsNotJson() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return 400 when JSON is malformed")
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(clientApplicationService, never()).createPerson(anyString(), anyString(), anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return response with proper Content-Type")
    void shouldReturnProperContentType() throws Exception {
        // Given
        CreatePersonRequest request = new CreatePersonRequest(
                "John Doe",
                "john.doe@example.com",
                "+33123456789",
                LocalDate.of(1990, 1, 15)
        );

        UUID personId = UUID.randomUUID();
        Person savedPerson = new Person(
                ClientName.of("John Doe"),
                Email.of("john.doe@example.com"),
                PhoneNumber.of("+33123456789"),
                LocalDate.of(1990, 1, 15)
        );

        when(clientApplicationService.createPerson(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(savedPerson);

        // When / Then
        mockMvc.perform(post("/v1/clients/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}

