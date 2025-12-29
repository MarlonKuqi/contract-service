package com.mk.contractservice.infrastructure.web.contract;

import com.mk.contractservice.application.feature.contract.create.CreateContract;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractDtoMapper;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreateContractController.class)
@ActiveProfiles("test")
@DisplayName("CreateContractController - Tests WebMvc")
class CreateContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateContract createContract;

    @MockitoBean
    private ContractDtoMapper contractMapper;

    @Nested
    @DisplayName("POST /api/v2/contracts - Création de contrat")
    class CreateContractTests {

        @Test
        @DisplayName("GIVEN requête valide WHEN POST THEN retourne 201 avec Location header")
        void shouldCreateContractSuccessfully() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2026, 1, 1, 0, 0);
            BigDecimal costAmount = new BigDecimal("1500.00");

            Contract contract = Contract.builder()
                    .id(contractId)
                    .clientId(clientId)
                    .period(ContractPeriod.of(startDate, endDate))
                    .costAmount(ContractCost.of(costAmount))
                    .build();

            when(createContract.execute(any(CreateContract.Command.class))).thenReturn(contract);
            when(contractMapper.toResponse(contract)).thenReturn(
                    new ContractResponse(
                            contractId,
                            clientId,
                            startDate,
                            endDate,
                            true,
                            costAmount
                    )
            );

            String requestBody = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1500.00
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/v2/contracts/" + contractId))
                    .andExpect(jsonPath("$.id").value(contractId.toString()))
                    .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                    .andExpect(jsonPath("$.costAmount").value(1500.00))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("GIVEN startDate null WHEN POST THEN retourne 201 avec date par défaut")
        void shouldCreateContractWithDefaultStartDate() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime endDate = LocalDateTime.of(2026, 1, 1, 0, 0);
            BigDecimal costAmount = new BigDecimal("1500.00");

            Contract contract = Contract.builder()
                    .id(contractId)
                    .clientId(clientId)
                    .period(ContractPeriod.of(LocalDateTime.now(), endDate))
                    .costAmount(ContractCost.of(costAmount))
                    .build();

            when(createContract.execute(any(CreateContract.Command.class))).thenReturn(contract);
            when(contractMapper.toResponse(any(Contract.class))).thenReturn(
                    new ContractResponse(
                            contractId,
                            clientId,
                            LocalDateTime.now(),
                            endDate,
                            true,
                            costAmount
                    )
            );

            String requestBody = """
                    {
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1500.00
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(contractId.toString()))
                    .andExpect(jsonPath("$.clientId").value(clientId.toString()));
        }

        @Test
        @DisplayName("GIVEN endDate null WHEN POST THEN retourne 201 avec contrat actif indéfini")
        void shouldCreateContractWithNoEndDate() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
            BigDecimal costAmount = new BigDecimal("1500.00");

            Contract contract = Contract.builder()
                    .id(contractId)
                    .clientId(clientId)
                    .period(ContractPeriod.of(startDate, null))
                    .costAmount(ContractCost.of(costAmount))
                    .build();

            when(createContract.execute(any(CreateContract.Command.class))).thenReturn(contract);
            when(contractMapper.toResponse(contract)).thenReturn(
                    new com.mk.contractservice.infrastructure.web.contract.shared.ContractResponse(
                            contractId,
                            clientId,
                            startDate,
                            null,
                            true,
                            costAmount
                    )
            );

            String requestBody = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "costAmount": 1500.00
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.endDate").isEmpty())
                    .andExpect(jsonPath("$.active").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v2/contracts - Validation des erreurs")
    class ValidationErrorTests {

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                GIVEN costAmount null WHEN POST THEN retourne 422 | {"startDate":"2025-01-01T00:00:00","endDate":"2026-01-01T00:00:00"}
                GIVEN costAmount zéro WHEN POST THEN retourne 422 | {"startDate":"2025-01-01T00:00:00","endDate":"2026-01-01T00:00:00","costAmount":0}
                GIVEN costAmount négatif WHEN POST THEN retourne 422 | {"startDate":"2025-01-01T00:00:00","endDate":"2026-01-01T00:00:00","costAmount":-1500.00}
                """)
        void shouldRejectInvalidCostAmount(String displayName, String requestBody) throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableContent());
        }

        @Test
        @DisplayName("GIVEN clientId manquant WHEN POST THEN retourne 400")
        void shouldRejectMissingClientId() throws Exception {
            // Given
            String requestBody = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1500.00
                    }
                    """;

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN POST THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            String requestBody = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1500.00
                    }
                    """;

            when(createContract.execute(any(CreateContract.Command.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(post(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }
}

