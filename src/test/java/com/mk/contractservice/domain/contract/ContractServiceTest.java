package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.contract.service.ContractValidationService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService - Unit Tests")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractValidationService contractValidationService;

    @InjectMocks
    private ContractService contractService;

    @Nested
    @DisplayName("getContractForClient() - Happy Path")
    class GetContractForClientHappyPath {

        @Test
        @DisplayName("GIVEN valid clientId and contractId WHEN getContractForClient THEN return contract")
        void shouldReturnContractForClient() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            Contract expectedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(now, now.plusMonths(12)),
                    ContractCost.of(new BigDecimal("1000.00"))
            );

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(expectedContract));
            doNothing().when(contractValidationService).ensureContractBelongsToClient(expectedContract, clientId);

            // When
            Contract result = contractService.getContractForClient(clientId, contractId);

            // Then
            assertThat(result).isEqualTo(expectedContract);
            assertThat(result.getClientId()).isEqualTo(clientId);

            verify(contractRepository).findById(contractId);
            verify(contractValidationService).ensureContractBelongsToClient(expectedContract, clientId);
        }
    }

    @Nested
    @DisplayName("getContractForClient() - Error Cases")
    class GetContractForClientErrorCases {

        @Test
        @DisplayName("GIVEN non-existent contractId WHEN getContractForClient THEN throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contractService.getContractForClient(clientId, contractId))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(contractId.toString());

            verify(contractRepository).findById(contractId);
            verifyNoInteractions(contractValidationService);
        }
    }

    @Nested
    @DisplayName("getActiveContractsForClient() - Pagination")
    class GetActiveContractsForClientPagination {

        @Test
        @DisplayName("GIVEN valid pagination WHEN getActiveContractsForClient THEN return page of contracts")
        void shouldReturnPageOfActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            LocalDateTime now = LocalDateTime.now();

            List<Contract> contracts = List.of(
                    Contract.of(clientId, ContractPeriod.of(now, null), ContractCost.of(new BigDecimal("1000.00"))),
                    Contract.of(clientId, ContractPeriod.of(now.minusMonths(1), null), ContractCost.of(new BigDecimal("2000.00")))
            );

            Page<Contract> expectedPage = new PageImpl<>(contracts, pageable, 2);

            when(contractRepository.findActiveByClientIdPageable(clientId, null, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<Contract> result = contractService.getActiveContractsForClient(clientId, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getNumber()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(contractRepository).findActiveByClientIdPageable(clientId, null, pageable);
        }

        @Test
        @DisplayName("GIVEN updatedSince filter WHEN getActiveContractsForClient THEN return filtered contracts")
        void shouldReturnFilteredContractsByUpdatedSince() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime updatedSince = LocalDateTime.now().minusDays(7);
            Pageable pageable = PageRequest.of(0, 20);

            List<Contract> contracts = List.of(
                    Contract.of(clientId, ContractPeriod.of(LocalDateTime.now(), null), ContractCost.of(new BigDecimal("1000.00")))
            );

            Page<Contract> expectedPage = new PageImpl<>(contracts, pageable, 1);

            when(contractRepository.findActiveByClientIdPageable(clientId, updatedSince, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<Contract> result = contractService.getActiveContractsForClient(clientId, updatedSince, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(contractRepository).findActiveByClientIdPageable(clientId, updatedSince, pageable);
        }

        @Test
        @DisplayName("GIVEN no active contracts WHEN getActiveContractsForClient THEN return empty page")
        void shouldReturnEmptyPageWhenNoActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);

            Page<Contract> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(contractRepository.findActiveByClientIdPageable(clientId, null, pageable))
                    .thenReturn(emptyPage);

            // When
            Page<Contract> result = contractService.getActiveContractsForClient(clientId, null, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();

            verify(contractRepository).findActiveByClientIdPageable(clientId, null, pageable);
        }
    }

    @Nested
    @DisplayName("sumActiveContractsForClient()")
    class SumActiveContractsForClient {

        @Test
        @DisplayName("GIVEN active contracts WHEN sumActiveContractsForClient THEN return total sum")
        void shouldReturnSumOfActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            BigDecimal expectedSum = new BigDecimal("5500.00");

            when(contractRepository.sumActiveByClientId(clientId)).thenReturn(expectedSum);

            // When
            BigDecimal result = contractService.sumActiveContractsForClient(clientId);

            // Then
            assertThat(result).isEqualByComparingTo(expectedSum);

            verify(contractRepository).sumActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN no active contracts WHEN sumActiveContractsForClient THEN return zero")
        void shouldReturnZeroWhenNoActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();

            when(contractRepository.sumActiveByClientId(clientId)).thenReturn(BigDecimal.ZERO);

            // When
            BigDecimal result = contractService.sumActiveContractsForClient(clientId);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);

            verify(contractRepository).sumActiveByClientId(clientId);
        }
    }
}
