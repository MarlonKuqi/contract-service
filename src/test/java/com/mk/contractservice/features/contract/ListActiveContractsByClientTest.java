package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.features.shared.InvalidPageRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(null, null, 0, 20))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 20, 'Page number must not be less than zero'",
            "0, 0, 'Page size must not be less than one'",
            "0, -1, 'Page size must not be less than one'",
            "0, 101, 'Page size must not exceed 100'"
    })
    @DisplayName("GIVEN page or size out of bounds WHEN building Query THEN throw InvalidPageRequestException")
    void shouldRejectPageRequestOutOfBounds(int page, int size, String expectedMessage) {
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(UUID.randomUUID(), null, page, size))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    @DisplayName("GIVEN size equal to the maximum WHEN building Query THEN accept it")
    void shouldAcceptMaxPageSize() {
        assertThatCode(() -> new ListActiveContractsByClient.Query(UUID.randomUUID(), null, 0, ListActiveContractsByClient.MAX_PAGE_SIZE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN page and size WHEN execute THEN request that page sorted by lastModified descending")
    void shouldRequestPageSortedByLastModifiedDescending() {
        UUID clientId = UUID.randomUUID();
        ContractSearchCriteria expectedCriteria = ContractSearchCriteria.activeForClient(clientId, LocalDateTime.now(FIXED_CLOCK));
        Pageable expectedPageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "lastModified"));
        Page<Contract> expectedPage = Page.empty();

        when(contractRepository.findByCriteria(expectedCriteria, expectedPageable))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, null, 2, 10));

        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    @DisplayName("GIVEN non-existent clientId WHEN execute THEN throw ClientNotFoundException")
    void shouldThrowWhenClientNotFound() {
        UUID unknownId = UUID.randomUUID();
        doThrow(ClientNotFoundException.forId(unknownId)).when(clientRepository).ensureExists(unknownId);

        assertThatThrownBy(() -> handler.execute(new ListActiveContractsByClient.Query(unknownId, null, 0, 20)))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN existing client WHEN execute THEN return contracts active at clock time")
    void shouldReturnContractsPageForExistingClient() {
        UUID clientId = UUID.randomUUID();
        Page<Contract> expectedPage = Page.empty();
        ContractSearchCriteria expectedCriteria = ContractSearchCriteria.activeForClient(clientId, LocalDateTime.now(FIXED_CLOCK));

        when(contractRepository.findByCriteria(eq(expectedCriteria), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, null, 0, 20));

        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    @DisplayName("GIVEN existing client and updatedSince WHEN execute THEN return contracts active at clock time and updated since the given date")
    void shouldFilterActiveContractsByUpdateDate() {
        UUID clientId = UUID.randomUUID();
        LocalDateTime updatedSince = LocalDateTime.now(FIXED_CLOCK).minusDays(7);
        Page<Contract> expectedPage = Page.empty();
        ContractSearchCriteria expectedCriteria = new ContractSearchCriteria(clientId, LocalDateTime.now(FIXED_CLOCK), updatedSince);

        when(contractRepository.findByCriteria(eq(expectedCriteria), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, updatedSince, 0, 20));

        assertThat(result).isEqualTo(expectedPage);
    }
}

