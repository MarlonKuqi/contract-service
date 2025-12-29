package com.mk.contractservice.infrastructure.web.client;

import com.mk.contractservice.application.feature.client.update.UpdateClient;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.client.shared.ClientEndpoints;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpdateClientController.class)
@ActiveProfiles("test")
@DisplayName("UpdateClientController - Tests WebMvc")
class UpdateClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UpdateClient updateClient;

    @Nested
    @DisplayName("PUT /api/v2/clients/{id} - Mise à jour complète de client")
    class UpdateClientTests {

        @Test
        @DisplayName("GIVEN requête valide WHEN PUT THEN retourne 204")
        void shouldUpdateClientSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "name": "Alice Martin Updated",
                        "email": "alice.updated@example.com",
                        "phone": "+41791234999"
                    }
                    """;

            when(updateClient.execute(any(UpdateClient.Command.class))).thenReturn(null);

            // When & Then
            mockMvc.perform(put(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN PUT THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "name": "Alice Martin",
                        "email": "alice@example.com",
                        "phone": "+41791234567"
                    }
                    """;

            when(updateClient.execute(any(UpdateClient.Command.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(put(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                GIVEN name manquant WHEN PUT THEN retourne 422 | {"email":"alice@example.com","phone":"+41791234567"}
                GIVEN email manquant WHEN PUT THEN retourne 422 | {"name":"Alice Martin","phone":"+41791234567"}
                GIVEN phone manquant WHEN PUT THEN retourne 422 | {"name":"Alice Martin","email":"alice@example.com"}
                GIVEN email invalide WHEN PUT THEN retourne 422 | {"name":"Alice Martin","email":"invalid-email","phone":"+41791234567"}
                GIVEN phone invalide WHEN PUT THEN retourne 422 | {"name":"Alice Martin","email":"alice@example.com","phone":"123"}
                """)
        void shouldRejectInvalidData(String displayName, String requestBody) throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(put(ClientEndpoints.CLIENTS_BASE + "/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent());
        }
    }
}

