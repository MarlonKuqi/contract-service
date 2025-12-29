package com.mk.contractservice.infrastructure.web.contract;

import com.mk.contractservice.application.feature.contract.search.GetContractById;
import com.mk.contractservice.application.feature.contract.search.ListActiveContractsByClient;
import com.mk.contractservice.application.feature.contract.search.SumActiveContractsByClient;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractDtoMapper;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchContractController.class)
@ActiveProfiles("test")
@DisplayName("SearchContractController - Tests WebMvc")
class SearchContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetContractById getContractById;

    @MockitoBean
    private ListActiveContractsByClient listActiveContractsByClient;

    @MockitoBean
    private SumActiveContractsByClient sumActiveContractsByClient;

    @MockitoBean
    private ContractDtoMapper contractMapper;

    @Nested
    @DisplayName("GET /api/v2/contracts/{contractId} - Recherche par ID")
    class GetContractByIdTests {

        @Test
        @DisplayName("GIVEN contrat existant WHEN GET THEN retourne 200")
        void shouldReturnContractWhenExists() throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusYears(1);
            BigDecimal costAmount = new BigDecimal("1500.00");

            Contract contract = Contract.builder()
                    .id(contractId)
                    .clientId(clientId)
                    .period(ContractPeriod.of(startDate, endDate))
                    .costAmount(ContractCost.of(costAmount))
                    .build();

            when(getContractById.execute(any(GetContractById.Query.class))).thenReturn(contract);
            when(contractMapper.toResponse(contract)).thenReturn(
                    new ContractResponse(contractId, clientId, startDate, endDate, true, costAmount)
            );

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE + "/{contractId}", contractId)
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(contractId.toString()))
                    .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                    .andExpect(jsonPath("$.costAmount").value(1500.00));
        }

        @Test
        @DisplayName("GIVEN contrat inexistant WHEN GET THEN retourne 404")
        void shouldReturn404WhenContractNotFound() throws Exception {
            // Given
            UUID contractId = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            when(getContractById.execute(any(GetContractById.Query.class)))
                    .thenThrow(new ContractNotFoundException("Contract not found: " + contractId));

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE + "/{contractId}", contractId)
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/contracts - Liste paginée")
    class ListActiveContractsTests {

        @Test
        @DisplayName("GIVEN contrats actifs WHEN GET THEN retourne 200")
        void shouldReturnActiveContracts() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusYears(1);
            BigDecimal costAmount = new BigDecimal("1500.00");

            Contract contract = Contract.builder()
                    .id(contractId)
                    .clientId(clientId)
                    .period(ContractPeriod.of(startDate, endDate))
                    .costAmount(ContractCost.of(costAmount))
                    .build();

            Page<Contract> page = new PageImpl<>(List.of(contract), PageRequest.of(0, 20), 1);
            when(listActiveContractsByClient.execute(any(ListActiveContractsByClient.Query.class)))
                    .thenReturn(page);
            when(contractMapper.toResponse(contract)).thenReturn(
                    new ContractResponse(contractId, clientId, startDate, endDate, true, costAmount)
            );

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(contractId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GIVEN client inexistant WHEN GET THEN retourne 404")
        void shouldReturn404WhenClientNotFound() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();

            when(listActiveContractsByClient.execute(any(ListActiveContractsByClient.Query.class)))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE)
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/contracts/sum - Somme des coûts")
    class SumActiveContractsTests {

        @Test
        @DisplayName("GIVEN contrats actifs WHEN GET sum THEN retourne 200 avec la somme")
        void shouldReturnSumOfActiveCosts() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();
            BigDecimal sum = new BigDecimal("4500.00");

            when(sumActiveContractsByClient.execute(any(SumActiveContractsByClient.Query.class)))
                    .thenReturn(sum);

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE + "/sum")
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(4500.00));
        }

        @Test
        @DisplayName("GIVEN aucun contrat actif WHEN GET sum THEN retourne 200 avec 0")
        void shouldReturnZeroWhenNoActiveContracts() throws Exception {
            // Given
            UUID clientId = UUID.randomUUID();

            when(sumActiveContractsByClient.execute(any(SumActiveContractsByClient.Query.class)))
                    .thenReturn(BigDecimal.ZERO);

            // When & Then
            mockMvc.perform(get(ContractEndpoints.CONTRACTS_BASE + "/sum")
                            .param("clientId", clientId.toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(0));
        }
    }
}

