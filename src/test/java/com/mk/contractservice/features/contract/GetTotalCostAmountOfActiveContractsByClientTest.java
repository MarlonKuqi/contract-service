package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTotalCostAmountOfActiveContractsByClient Handler")
class GetTotalCostAmountOfActiveContractsByClientTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractQueryPort contractQueryPort;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private GetTotalCostAmountOfActiveContractsByClient.Handler handler;

    @BeforeEach
    void setUp() {
        ClientValidationService clientValidationService = new ClientValidationService(clientRepository);
        handler = new GetTotalCostAmountOfActiveContractsByClient.Handler(clientValidationService, contractQueryPort, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN null clientId WHEN building Query THEN throw NullPointerException")
    void shouldThrowWhenClientIdIsNull() {
        assertThatThrownBy(() -> new GetTotalCostAmountOfActiveContractsByClient.Query(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN non-existent clientId WHEN execute THEN throw ClientNotFoundException")
    void shouldThrowWhenClientNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(clientRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> handler.execute(new GetTotalCostAmountOfActiveContractsByClient.Query(unknownId)))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN existing client WHEN execute THEN return total cost amount active at clock time")
    void shouldReturnTotalCostForExistingClient() {
        UUID clientId = UUID.randomUUID();
        BigDecimal expectedTotal = new BigDecimal("1500.00");

        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(contractQueryPort.calculateTotalCostAmountForClient(clientId, LocalDateTime.now(FIXED_CLOCK)))
                .thenReturn(expectedTotal);

        BigDecimal result = handler.execute(new GetTotalCostAmountOfActiveContractsByClient.Query(clientId));

        assertThat(result).isEqualByComparingTo(expectedTotal);
    }
}

