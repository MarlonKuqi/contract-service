package com.mk.contractservice.infrastructure.web.contract;

import com.mk.contractservice.application.feature.contract.patchcost.PatchContractCost;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatchContractCostController.class)
@ActiveProfiles("test")
@DisplayName("PatchContractCostController - Tests WebMvc")
class PatchContractCostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatchContractCost patchContractCost;

    @Nested
    @DisplayName("PATCH /api/v2/contracts/{contractId}/cost - Mise à jour du coût")
    class PatchContractCostTests {

        @Test
        @DisplayName("GIVEN montant valide WHEN PATCH THEN retourne 204")
        void shouldPatchCostSuccessfully() throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "amount": 2500.00
                    }
                    """;

            when(patchContractCost.execute(any(PatchContractCost.Command.class))).thenReturn(null);

            // When & Then
            mockMvc.perform(patch(ContractEndpoints.CONTRACTS_BASE + "/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GIVEN contrat inexistant WHEN PATCH THEN retourne 404")
        void shouldReturn404WhenContractNotFound() throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "amount": 2500.00
                    }
                    """;

            when(patchContractCost.execute(any(PatchContractCost.Command.class)))
                    .thenThrow(new ContractNotFoundException("Contract not found: " + contractId));

            // When & Then
            mockMvc.perform(patch(ContractEndpoints.CONTRACTS_BASE + "/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN PATCH THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "amount": 2500.00
                    }
                    """;

            when(patchContractCost.execute(any(PatchContractCost.Command.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(patch(ContractEndpoints.CONTRACTS_BASE + "/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                GIVEN amount null WHEN PATCH THEN retourne 422 | {}
                GIVEN amount négatif WHEN PATCH THEN retourne 422 | {"amount":-1500.00}
                GIVEN amount zéro WHEN PATCH THEN retourne 422 | {"amount":0}
                """)
        void shouldRejectInvalidAmount(String displayName, String requestBody) throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(patch(ContractEndpoints.CONTRACTS_BASE + "/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent());
        }
    }
}

