package com.mk.contractservice.infrastructure.web.client;

import com.mk.contractservice.application.feature.client.patch.PatchClient;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatchClientController.class)
@ActiveProfiles("test")
@DisplayName("PatchClientController - Tests WebMvc")
class PatchClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatchClient patchClient;

    @Nested
    @DisplayName("PATCH /api/v2/clients/{id} - Mise à jour partielle de client")
    class PatchClientTests {

        @Test
        @DisplayName("GIVEN tous les champs WHEN PATCH THEN retourne 204")
        void shouldPatchAllFieldsSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "name": "Alice Martin Updated",
                        "email": "alice.updated@example.com",
                        "phone": "+41791234999"
                    }
                    """;

            when(patchClient.execute(any(PatchClient.Command.class))).thenReturn(null);

            // When & Then
            mockMvc.perform(patch(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN seulement name WHEN PATCH THEN retourne 204")
        void shouldPatchOnlyNameSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "name": "Alice Martin Updated"
                    }
                    """;

            when(patchClient.execute(any(PatchClient.Command.class))).thenReturn(null);

            // When & Then
            mockMvc.perform(patch(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN aucun champ WHEN PATCH THEN retourne 204 (idempotent)")
        void shouldPatchWithNoFieldsSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = "{}";

            when(patchClient.execute(any(PatchClient.Command.class))).thenReturn(null);

            // When & Then
            mockMvc.perform(patch(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN PATCH THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "name": "Alice Martin"
                    }
                    """;

            when(patchClient.execute(any(PatchClient.Command.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(patch(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }
}

