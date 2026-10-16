package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListActiveContractsByClient Handler")
class ListActiveContractsByClientTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private ListActiveContractsByClient.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new ListActiveContractsByClient.Handler(clientRepository, contractRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN null clientId WHEN building Query THEN throw NullPointerException")
    void shouldThrowWhenClientIdIsNull() {
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(null, null, Pageable.unpaged()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN null pageable WHEN building Query THEN throw NullPointerException")
    void shouldThrowWhenPageableIsNull() {
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(UUID.randomUUID(), null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN non-existent clientId WHEN execute THEN throw ClientNotFoundException")
    void shouldThrowWhenClientNotFound() {
        UUID unknownId = UUID.randomUUID();
        doThrow(ClientNotFoundException.forId(unknownId)).when(clientRepository).ensureExists(unknownId);

        assertThatThrownBy(() -> handler.execute(new ListActiveContractsByClient.Query(unknownId, null, Pageable.unpaged())))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN existing client WHEN execute THEN return contracts active at clock time")
    void shouldReturnContractsPageForExistingClient() {
        UUID clientId = UUID.randomUUID();
        Page<Contract> expectedPage = Page.empty();
        ContractSearchCriteria expectedCriteria = ContractSearchCriteria.activeForClient(clientId, LocalDateTime.now(FIXED_CLOCK));

        when(contractRepository.findByCriteria(expectedCriteria, Pageable.unpaged()))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, null, Pageable.unpaged()));

        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    @DisplayName("GIVEN existing client and updatedSince WHEN execute THEN return contracts active at clock time and updated since the given date")
    void shouldFilterActiveContractsByUpdateDate() {
        UUID clientId = UUID.randomUUID();
        LocalDateTime updatedSince = LocalDateTime.now(FIXED_CLOCK).minusDays(7);
        Page<Contract> expectedPage = Page.empty();
        ContractSearchCriteria expectedCriteria = new ContractSearchCriteria(clientId, LocalDateTime.now(FIXED_CLOCK), updatedSince);

        when(contractRepository.findByCriteria(expectedCriteria, Pageable.unpaged()))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, updatedSince, Pageable.unpaged()));

        assertThat(result).isEqualTo(expectedPage);
    }
}

