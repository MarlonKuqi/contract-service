package com.mk.contractservice.infrastructure.web.client;

import com.mk.contractservice.application.feature.client.search.GetClientById;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.client.shared.ClientDtoMapper;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import com.mk.contractservice.infrastructure.web.client.shared.PersonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchClientController.class)
@ActiveProfiles("test")
@DisplayName("SearchClientController - Tests WebMvc")
class SearchClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetClientById getClientById;

    @MockitoBean
    private ClientDtoMapper clientDtoMapper;

    @Nested
    @DisplayName("GET /api/v2/clients/{id} - Recherche de client")
    class GetClientByIdTests {

        @Test
        @DisplayName("GIVEN client existant WHEN GET THEN retourne 200 avec le client")
        void shouldReturnClientWhenExists() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            Person person = Person.builder()
                    .id(clientId)
                    .name(ClientName.of("Alice Martin"))
                    .email(ClientEmail.of("alice.martin@example.com"))
                    .phone(ClientPhoneNumber.of("+41791234567"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                    .build();

            when(getClientById.execute(any(GetClientById.Query.class))).thenReturn(person);
            when(clientDtoMapper.toResponse(person)).thenReturn(
                    new PersonResponse(
                            clientId,
                            "Alice Martin",
                            "alice.martin@example.com",
                            "+41791234567",
                            LocalDate.of(1990, 5, 15)
                    )
            );

            // When & Then
            mockMvc.perform(get(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(clientId.toString()))
                    .andExpect(jsonPath("$.name").value("Alice Martin"))
                    .andExpect(jsonPath("$.email").value("alice.martin@example.com"))
                    .andExpect(jsonPath("$.phone").value("+41791234567"))
                    .andExpect(jsonPath("$.birthDate").value("1990-05-15"));
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN GET THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();

            when(getClientById.execute(any(GetClientById.Query.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(get(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}

