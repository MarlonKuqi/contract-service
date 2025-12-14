package com.mk.contractservice.web.controller;


import com.mk.contractservice.application.client.ClientApplicationService;
import com.mk.contractservice.application.client.dto.PersonDto;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.web.GlobalExceptionHandler;
import com.mk.contractservice.web.WebMvcConfig;
import com.mk.contractservice.web.client.ClientController;
import com.mk.contractservice.web.client.ClientControllerAdvice;
import com.mk.contractservice.web.client.mapper.ClientDtoMapperImpl;
import com.mk.contractservice.web.client.mapper.CompanyResponseMapperImpl;
import com.mk.contractservice.web.client.mapper.PersonResponseMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@ContextConfiguration(classes = {
        ClientController.class,
        ClientControllerAdvice.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class
})
@Import({
        ClientDtoMapperImpl.class,
        PersonResponseMapperImpl.class,
        CompanyResponseMapperImpl.class
})
@DisplayName("ClientController - MockMvc Tests")
@ActiveProfiles("test")
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientApplicationService clientService;


    @Nested
    @DisplayName("POST /v2/clients - Create Client")
    class CreateClientTests {

        @Test
        @DisplayName("GIVEN valid person request WHEN create THEN return 201 with Location header")
        void shouldCreatePersonSuccessfully() throws Exception {

            UUID clientId = UUID.randomUUID();
            PersonDto personDto = new PersonDto(
                    clientId,
                    "John Doe",
                    "john.doe@example.com",
                    "+41791234567",
                    LocalDate.of(1990, 1, 1)
            );

            String requestJson = """
                    {
                        "type": "PERSON",
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "phone": "+41791234567",
                        "birthDate": "1990-01-01"
                    }
                    """;

            when(clientService.createPerson(anyString(), anyString(), anyString(), any(LocalDate.class)))
                    .thenReturn(personDto);

            mockMvc.perform(post("/v2/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/v2/clients/" + clientId))
                    .andExpect(jsonPath("$.id").value(clientId.toString()))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"));

            verify(clientService).createPerson("John Doe", "john.doe@example.com", "+41791234567", LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("GIVEN duplicate clientEmail WHEN create THEN return 409 Conflict")
        void shouldReturnConflictWhenEmailExists() throws Exception {
            String requestJson = """
                    {
                        "type": "PERSON",
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "phone": "+41791234567",
                        "birthDate": "1990-01-01"
                    }
                    """;

            when(clientService.createPerson(anyString(), anyString(), anyString(), any(LocalDate.class)))
                    .thenThrow(new ClientAlreadyExistsException("Client already exists"));


            mockMvc.perform(post("/v2/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Client Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Client already exists"));
        }

        @Test
        @DisplayName("GIVEN invalid clientEmail format WHEN create THEN return 422 Unprocessable Entity")
        void shouldReturnUnprocessableEntityForInvalidEmail() throws Exception {
            String requestJson = """
                    {
                        "type": "PERSON",
                        "name": "John Doe",
                        "email": "invalid-email",
                        "phone": "+41791234567",
                        "birthDate": "1990-01-01"
                    }
                    """;

            when(clientService.createPerson(anyString(), anyString(), anyString(), any(LocalDate.class)))
                    .thenThrow(new IllegalArgumentException("Invalid clientEmail format"));

            mockMvc.perform(post("/v2/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("GIVEN missing required fields WHEN create THEN return 422 Unprocessable Entity")
        void shouldReturnBadRequestForMissingField() throws Exception {
            String invalidJson = """
                    {
                        "type": "PERSON",
                        "email": "john.doe@example.com",
                        "phone": "+41791234567"
                    }
                    """;

            mockMvc.perform(post("/v2/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.validations").isArray());
        }
    }

    @Nested
    @DisplayName("GET /v2/clients/{id} - Get Client by ID")
    class GetClientTests {

        @Test
        @DisplayName("GIVEN existing client WHEN get by id THEN return 200 with client data")
        void shouldReturnClientWhenExists() throws Exception {
            UUID clientId = UUID.randomUUID();
            PersonDto personDto = new PersonDto(
                    clientId,
                    "John Doe",
                    "john.doe@example.com",
                    "+41791234567",
                    LocalDate.of(1990, 1, 1)
            );

            when(clientService.getClientById(clientId)).thenReturn(personDto);

            mockMvc.perform(get("/v2/clients/{id}", clientId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(clientId.toString()))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.phone").value("+41791234567"));

            verify(clientService).getClientById(clientId);
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN get by id THEN return 404 Not Found")
        void shouldReturn404WhenClientNotFound() throws Exception {
            UUID clientId = UUID.randomUUID();

            when(clientService.getClientById(clientId))
                    .thenThrow(new ClientNotFoundException("Client with ID " + clientId + " not found"));

            mockMvc.perform(get("/v2/clients/{id}", clientId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Client Not Found"))
                    .andExpect(jsonPath("$.detail").value("Client with ID " + clientId + " not found"));
        }
    }

    @Nested
    @DisplayName("PUT /v2/clients/{id} - Update Client")
    class UpdateClientTests {

        @Test
        @DisplayName("GIVEN valid update request WHEN update THEN return 204 No Content")
        void shouldUpdateClientSuccessfully() throws Exception {
            UUID clientId = UUID.randomUUID();
            String updateJson = """
                    {
                        "name": "Jane Doe Updated",
                        "email": "jane.updated@example.com",
                        "phone": "+41799999999"
                    }
                    """;

            mockMvc.perform(put("/v2/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNoContent());

            verify(clientService).updateCommonFields(eq(clientId), any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN update THEN return 404 Not Found")
        void shouldReturn404WhenUpdatingNonExistentClient() throws Exception {
            UUID clientId = UUID.randomUUID();
            String updateJson = """
                    {
                        "name": "Jane Doe",
                        "email": "jane@example.com",
                        "phone": "+41799999999"
                    }
                    """;

            when(clientService.updateCommonFields(any(), any(), any(), any()))
                    .thenThrow(new ClientNotFoundException("Client with ID " + clientId + " not found"));

            mockMvc.perform(put("/v2/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /v2/clients/{id} - Partial Update Client")
    class PatchClientTests {

        @Test
        @DisplayName("GIVEN valid patch request with name only WHEN patch THEN return 204 No Content")
        void shouldPatchClientSuccessfully() throws Exception {

            UUID clientId = UUID.randomUUID();
            String patchJson = """
                    {
                        "name": "John Updated"
                    }
                    """;


            mockMvc.perform(patch("/v2/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isNoContent());

            verify(clientService).patchClient(eq(clientId), any(), eq(null), eq(null));
        }

        @Test
        @DisplayName("GIVEN valid patch request with clientEmail only WHEN patch THEN return 204 No Content")
        void shouldPatchClientEmailOnly() throws Exception {

            UUID clientId = UUID.randomUUID();
            String patchJson = """
                    {
                        "email": "new.email@example.com"
                    }
                    """;


            mockMvc.perform(patch("/v2/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isNoContent());

            verify(clientService).patchClient(eq(clientId), eq(null), any(), eq(null));
        }
    }
}

