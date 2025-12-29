package com.mk.contractservice.infrastructure.web.client;

import com.mk.contractservice.application.feature.client.delete.DeleteClient;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeleteClientController.class)
@ActiveProfiles("test")
@DisplayName("DeleteClientController - Tests WebMvc")
class DeleteClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteClient deleteClient;

    @Nested
    @DisplayName("DELETE /api/v2/clients/{id} - Suppression de client")
    class DeleteClientTests {

        @Test
        @DisplayName("GIVEN client existant WHEN DELETE THEN retourne 204")
        void shouldDeleteClientSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            doNothing().when(deleteClient).execute(any(DeleteClient.Command.class));

            // When & Then
            mockMvc.perform(delete(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN DELETE THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            doThrow(new ClientNotFoundException("Client not found: " + clientId))
                    .when(deleteClient).execute(any(DeleteClient.Command.class));

            // When & Then
            mockMvc.perform(delete(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId))
                    .andExpect(status().isNotFound());
        }
    }
}

