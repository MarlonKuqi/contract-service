package com.mk.contractservice.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mk.contractservice.application.ContractApplicationService;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.advice.ContractControllerAdvice;
import com.mk.contractservice.web.advice.GlobalExceptionHandler;
import com.mk.contractservice.web.dto.mapper.common.ValueObjectMappersImpl;
import com.mk.contractservice.web.dto.mapper.contract.ContractMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@ContextConfiguration(classes = {
        ContractController.class,
        ContractControllerAdvice.class,
        GlobalExceptionHandler.class
})
@Import({
        ContractMapperImpl.class,
        ValueObjectMappersImpl.class
})
@DisplayName("ContractController - MockMvc Tests")
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContractApplicationService contractService;


    @Nested
    @DisplayName("POST /v2/contracts - Create Contract")
    class CreateContractTests {

        @Test
        @DisplayName("GIVEN valid contract request WHEN create THEN return 201 with Location header")
        void shouldCreateContractSuccessfully() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Person client = Person.reconstitute(
                    clientId,
                    ClientName.of("John Doe"),
                    Email.of("john.doe@example.com"),
                    PhoneNumber.of("+41791234567"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(client)
                    .period(ContractPeriod.of(
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                    ))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            String requestJson = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1000.00
                    }
                    """;

            when(contractService.createForClient(any(UUID.class), any(), any(), any()))
                    .thenReturn(contract);
            mockMvc.perform(post("/v2/contracts")
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/v2/contracts/" + contractId))
                    .andExpect(jsonPath("$.id").value(contractId.toString()))
                    .andExpect(jsonPath("$.costAmount").value(1000.00));

            verify(contractService).createForClient(any(UUID.class), any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN create contract THEN return 404 Not Found")
        void shouldReturn404WhenClientNotFound() throws Exception {

            UUID clientId = UUID.randomUUID();
            String requestJson = """
                    {
                        "startDate": "2025-01-01T00:00:00",
                        "endDate": "2026-01-01T00:00:00",
                        "costAmount": 1000.00
                    }
                    """;

            when(contractService.createForClient(any(), any(), any(), any()))
                    .thenThrow(new ClientNotFoundException("Client not found: " + clientId));

            mockMvc.perform(post("/v2/contracts")
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Client Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /v2/contracts/{contractId} - Get Contract by ID")
    class GetContractTests {

        @Test
        @DisplayName("GIVEN existing contract WHEN get by id THEN return 200 with contract data")
        void shouldReturnContractWhenExists() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Person client = Person.reconstitute(
                    clientId,
                    ClientName.of("John Doe"),
                    Email.of("john.doe@example.com"),
                    PhoneNumber.of("+41791234567"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(client)
                    .period(ContractPeriod.of(
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                    ))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            when(contractService.getContractById(clientId, contractId))
                    .thenReturn(contract);


            mockMvc.perform(get("/v2/contracts/{contractId}", contractId)
                            .param("clientId", clientId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(contractId.toString()))
                    .andExpect(jsonPath("$.costAmount").value(1000.00));

            verify(contractService).getContractById(clientId, contractId);
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN get by id THEN return 404 Not Found")
        void shouldReturn404WhenContractNotFound() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            when(contractService.getContractById(clientId, contractId))
                    .thenThrow(new ContractNotFoundException("Contract not found: " + contractId));


            mockMvc.perform(get("/v2/contracts/{contractId}", contractId)
                            .param("clientId", clientId.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Contract Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /v2/contracts - Get Active Contracts")
    class GetActiveContractsTests {

        @Test
        @DisplayName("GIVEN active contracts exist WHEN get contracts THEN return 200 with paginated list")
        void shouldReturnActiveContracts() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Person client = Person.reconstitute(
                    clientId,
                    ClientName.of("John Doe"),
                    Email.of("john.doe@example.com"),
                    PhoneNumber.of("+41791234567"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            Contract contract = Contract.builder()
                    .id(contractId)
                    .client(client)
                    .period(ContractPeriod.of(
                            LocalDateTime.of(2025, 1, 1, 0, 0),
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                    ))
                    .costAmount(ContractCost.of(new BigDecimal("1000.00")))
                    .build();

            Page<Contract> page = new PageImpl<>(List.of(contract), PageRequest.of(0, 20), 1);

            when(contractService.getActiveContractsPageable(eq(clientId), any(), any()))
                    .thenReturn(page);


            mockMvc.perform(get("/v2/contracts")
                            .param("clientId", clientId.toString())
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(contractId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(contractService).getActiveContractsPageable(eq(clientId), any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /v2/contracts/{contractId}/cost - Update Contract Cost")
    class UpdateCostTests {

        @Test
        @DisplayName("GIVEN valid cost update WHEN patch cost THEN return 204 No Content")
        void shouldUpdateCostSuccessfully() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            String costUpdateJson = """
                    {
                        "amount": 1500.00
                    }
                    """;


            mockMvc.perform(patch("/v2/contracts/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(costUpdateJson))
                    .andExpect(status().isNoContent());

            verify(contractService).updateCost(eq(clientId), eq(contractId), any());
        }

        @Test
        @DisplayName("GIVEN expired contract WHEN update cost THEN return 409 Conflict")
        void shouldReturn409WhenContractExpired() throws Exception {

            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            String costUpdateJson = """
                    {
                        "amount": 1500.00
                    }
                    """;

            when(contractService.updateCost(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                    .thenThrow(new ExpiredContractException(contractId));


            mockMvc.perform(patch("/v2/contracts/{contractId}/cost", contractId)
                            .param("clientId", clientId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(costUpdateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Expired Contract"));
        }
    }

    @Nested
    @DisplayName("GET /v2/contracts/sum - Sum Active Contracts")
    class SumContractsTests {

        @Test
        @DisplayName("GIVEN active contracts WHEN get sum THEN return 200 with total")
        void shouldReturnSumOfActiveContracts() throws Exception {

            UUID clientId = UUID.randomUUID();

            when(contractService.sumActiveContracts(clientId))
                    .thenReturn(new BigDecimal("5000.00"));


            mockMvc.perform(get("/v2/contracts/sum")
                            .param("clientId", clientId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5000.00"));

            verify(contractService).sumActiveContracts(clientId);
        }
    }
}

