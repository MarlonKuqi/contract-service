package com.mk.contractservice.application;

import com.mk.contractservice.application.contract.ContractApplicationService;
import com.mk.contractservice.application.contract.dto.ContractDto;
import com.mk.contractservice.application.contract.mapper.ContractMapper;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractApplicationService - Unit Tests")
class ContractApplicationServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractApplicationService service;

    private static final UUID JOHN_DOE_CLIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("Create Contract")
    class CreateContractTests {

        @Test
        @DisplayName("GIVEN valid client and contract data WHEN createForClient THEN contract is created")
        void shouldCreateContractForClient() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(30);
            BigDecimal amount = new BigDecimal("100.50");
            Contract savedContract = Contract.of(JOHN_DOE_CLIENT_ID, ContractPeriod.of(start, end), ContractCost.of(amount));
            ContractDto expectedDto = new ContractDto(UUID.randomUUID(), JOHN_DOE_CLIENT_ID, start, end, true, amount);

            when(clientRepository.existsById(JOHN_DOE_CLIENT_ID)).thenReturn(true);
            when(contractService.createAndPersistContract(eq(JOHN_DOE_CLIENT_ID), any(ContractPeriod.class), any(ContractCost.class)))
                    .thenReturn(savedContract);
            when(contractMapper.toDto(savedContract)).thenReturn(expectedDto);

            ContractDto result = service.createForClient(JOHN_DOE_CLIENT_ID, start, end, amount);

            assertThat(result).isNotNull();
            assertThat(result.clientId()).isEqualTo(JOHN_DOE_CLIENT_ID);
            assertThat(result.startDate()).isEqualTo(start);
            assertThat(result.endDate()).isEqualTo(end);
            assertThat(result.costAmount()).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("GIVEN null start date WHEN createForClient THEN use current date")
        void shouldUseCurrentDateWhenStartIsNull() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            LocalDateTime end = LocalDateTime.now().plusDays(30);
            BigDecimal amount = new BigDecimal("150.00");

            when(clientRepository.existsById(JOHN_DOE_CLIENT_ID)).thenReturn(true);
            when(contractService.createAndPersistContract(eq(JOHN_DOE_CLIENT_ID), any(ContractPeriod.class), any(ContractCost.class)))
                    .thenAnswer(invocation -> {
                        ContractPeriod period = invocation.getArgument(1);
                        ContractCost cost = invocation.getArgument(2);
                        return Contract.of(JOHN_DOE_CLIENT_ID, period, cost);
                    });
            when(contractMapper.toDto(any(Contract.class))).thenAnswer(invocation -> {
                Contract c = invocation.getArgument(0);
                return new ContractDto(UUID.randomUUID(), JOHN_DOE_CLIENT_ID, c.getPeriod().startDate(), end, true, amount);
            });

            ContractDto result = service.createForClient(JOHN_DOE_CLIENT_ID, null, end, amount);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(result.startDate()).isNotNull();
            assertThat(result.startDate()).isBetween(before, after);
        }

        @Test
        @DisplayName("GIVEN null end date WHEN createForClient THEN create with null end")
        void shouldAcceptNullEndDate() {
            LocalDateTime start = LocalDateTime.now();
            BigDecimal amount = new BigDecimal("100.00");
            Contract savedContract = Contract.of(JOHN_DOE_CLIENT_ID, ContractPeriod.of(start, null), ContractCost.of(amount));
            ContractDto expectedDto = new ContractDto(UUID.randomUUID(), JOHN_DOE_CLIENT_ID, start, null, true, amount);

            when(clientRepository.existsById(JOHN_DOE_CLIENT_ID)).thenReturn(true);
            when(contractService.createAndPersistContract(eq(JOHN_DOE_CLIENT_ID), any(ContractPeriod.class), any(ContractCost.class)))
                    .thenReturn(savedContract);
            when(contractMapper.toDto(savedContract)).thenReturn(expectedDto);

            ContractDto result = service.createForClient(JOHN_DOE_CLIENT_ID, start, null, amount);

            assertThat(result.endDate()).isNull();
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN createForClient THEN throw ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.existsById(nonExistentId)).thenReturn(false);

            assertThatThrownBy(() -> service.createForClient(nonExistentId, LocalDateTime.now(), null, BigDecimal.TEN))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("Client not found");

            verify(contractService, never()).createAndPersistContract(any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN valid data WHEN createForClient THEN contract is saved")
        void shouldSaveContractToRepository() {
            LocalDateTime start = LocalDateTime.now();
            BigDecimal amount = new BigDecimal("200.00");

            when(clientRepository.existsById(JOHN_DOE_CLIENT_ID)).thenReturn(true);
            when(contractService.createAndPersistContract(eq(JOHN_DOE_CLIENT_ID), any(ContractPeriod.class), any(ContractCost.class)))
                    .thenAnswer(invocation -> {
                        ContractPeriod period = invocation.getArgument(1);
                        ContractCost cost = invocation.getArgument(2);
                        return Contract.of(JOHN_DOE_CLIENT_ID, period, cost);
                    });
            when(contractMapper.toDto(any(Contract.class))).thenReturn(
                    new ContractDto(UUID.randomUUID(), JOHN_DOE_CLIENT_ID, start, null, true, amount)
            );

            service.createForClient(JOHN_DOE_CLIENT_ID, start, null, amount);

            ArgumentCaptor<ContractPeriod> periodCaptor = ArgumentCaptor.forClass(ContractPeriod.class);
            ArgumentCaptor<ContractCost> costCaptor = ArgumentCaptor.forClass(ContractCost.class);
            verify(contractService).createAndPersistContract(eq(JOHN_DOE_CLIENT_ID), periodCaptor.capture(), costCaptor.capture());

            ContractPeriod capturedPeriod = periodCaptor.getValue();
            ContractCost capturedCost = costCaptor.getValue();
            assertThat(capturedPeriod.startDate()).isEqualTo(start);
            assertThat(capturedPeriod.endDate()).isNull();
            assertThat(capturedCost.value()).isEqualByComparingTo(amount);
        }
    }

    @Nested
    @DisplayName("Update Contract Cost")
    class UpdateCostTests {

        @Test
        @DisplayName("GIVEN existing contract WHEN updateCost THEN cost is updated")
        void shouldUpdateCostForExistingContract() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Contract contract = Contract.reconstitute(
                    contractId,
                    JOHN_DOE_CLIENT_ID,
                    ContractPeriod.of(now, null),
                    ContractCost.of(new BigDecimal("100.00")));
            BigDecimal newAmount = new BigDecimal("200.00");
            ContractDto expectedDto = new ContractDto(contractId, JOHN_DOE_CLIENT_ID, now, null, true, newAmount);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(contractMapper.toDto(any(Contract.class))).thenReturn(expectedDto);

            ContractDto result = service.updateCost(JOHN_DOE_CLIENT_ID, contractId, newAmount);

            assertThat(result.costAmount()).isEqualByComparingTo(newAmount);
            assertThat(contract.getCostAmount().value()).isEqualByComparingTo(new BigDecimal("100.00"));
            verify(contractService).ensureContractBelongsToClient(contract, JOHN_DOE_CLIENT_ID);
            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN updateCost THEN throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            UUID clientId = UUID.randomUUID();
            UUID nonExistentId = UUID.randomUUID();
            when(contractRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCost(clientId, nonExistentId, BigDecimal.TEN))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(nonExistentId.toString());
        }


        @Test
        @DisplayName("GIVEN contract belonging to different client WHEN updateCost THEN throw ContractNotOwnedByClientException")
        void shouldThrowExceptionWhenContractBelongsToAnotherClient() {
            UUID contractId = UUID.randomUUID();
            UUID differentClientId = UUID.randomUUID();

            Contract contract = Contract.reconstitute(
                    contractId,
                    differentClientId,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("100.00")));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            // Mock the domain service to throw exception when validating ownership
            doThrow(new ContractNotOwnedByClientException(contractId, differentClientId))
                    .when(contractService).ensureContractBelongsToClient(contract, differentClientId);

            assertThatThrownBy(() -> service.updateCost(differentClientId, contractId, new BigDecimal("200.00")))
                    .isInstanceOf(ContractNotOwnedByClientException.class)
                    .hasMessageContaining(contractId.toString())
                    .hasMessageContaining(differentClientId.toString());
        }

        @Test
        @DisplayName("GIVEN expired contract WHEN updateCost THEN throw ExpiredContractException")
        void shouldThrowExceptionWhenContractIsExpired() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Contract expiredContract = Contract.reconstitute(
                    contractId,
                    JOHN_DOE_CLIENT_ID,
                    ContractPeriod.of(now.minusDays(100), now.minusDays(1)),
                    ContractCost.of(new BigDecimal("100.00")));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(expiredContract));

            assertThatThrownBy(() -> service.updateCost(JOHN_DOE_CLIENT_ID, contractId, new BigDecimal("200.00")))
                    .isInstanceOf(ExpiredContractException.class)
                    .hasMessageContaining(contractId.toString());
        }
    }

    @Nested
    @DisplayName("Get Contract By ID")
    class GetContractByIdTests {

        @Test
        @DisplayName("GIVEN contract exists and belongs to client WHEN getContractById THEN return contract")
        void shouldReturnContractWhenFoundAndBelongsToClient() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Contract contract = Contract.reconstitute(
                    contractId,
                    JOHN_DOE_CLIENT_ID,
                    ContractPeriod.of(now, null),
                    ContractCost.of(BigDecimal.valueOf(1000)));
            ContractDto expectedDto = new ContractDto(contractId, JOHN_DOE_CLIENT_ID, now, null, true, BigDecimal.valueOf(1000));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
            when(contractMapper.toDto(contract)).thenReturn(expectedDto);

            ContractDto result = service.getContractById(JOHN_DOE_CLIENT_ID, contractId);

            assertThat(result).isEqualTo(expectedDto);
            verify(contractService).ensureContractBelongsToClient(contract, JOHN_DOE_CLIENT_ID);
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN getContractById THEN throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            UUID contractId = UUID.randomUUID();

            when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getContractById(JOHN_DOE_CLIENT_ID, contractId))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(contractId.toString());
        }

        @Test
        @DisplayName("GIVEN contract belonging to different client WHEN getContractById THEN throw ContractNotOwnedByClientException")
        void shouldThrowExceptionWhenContractBelongsToDifferentClient() {
            UUID contractId = UUID.randomUUID();
            UUID differentClientId = UUID.randomUUID();

            Contract contract = Contract.reconstitute(
                    contractId,
                    differentClientId,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(BigDecimal.valueOf(1000)));

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            doThrow(new ContractNotOwnedByClientException(contractId, differentClientId))
                    .when(contractService).ensureContractBelongsToClient(contract, differentClientId);

            assertThatThrownBy(() -> service.getContractById(differentClientId, contractId))
                    .isInstanceOf(ContractNotOwnedByClientException.class)
                    .hasMessageContaining(contractId.toString())
                    .hasMessageContaining(differentClientId.toString());
        }
    }

    @Nested
    @DisplayName("Get Active Contracts")
    class GetActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN getActiveContractsPageable THEN return paginated active contracts")
        void shouldReturnActiveContracts() {
            LocalDateTime updatedSince = LocalDateTime.now().minusDays(7);
            LocalDateTime now = LocalDateTime.now();
            Contract contract = Contract.of(
                    JOHN_DOE_CLIENT_ID,
                    ContractPeriod.of(now, null),
                    ContractCost.of(BigDecimal.TEN));
            Page<Contract> contractPage = new PageImpl<>(List.of(contract));
            ContractDto contractDto = new ContractDto(UUID.randomUUID(), JOHN_DOE_CLIENT_ID, now, null, true, BigDecimal.TEN);
            Pageable pageable = PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(JOHN_DOE_CLIENT_ID), eq(updatedSince), eq(pageable)))
                    .thenReturn(contractPage);
            when(contractMapper.toDto(contract)).thenReturn(contractDto);

            Page<ContractDto> result = service.getActiveContractsPageable(JOHN_DOE_CLIENT_ID, updatedSince, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(contractDto);
        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN getActiveContractsPageable THEN return empty page")
        void shouldReturnEmptyPageWhenNoActiveContracts() {
            Page<Contract> emptyPage = Page.empty();
            Pageable pageable = PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(JOHN_DOE_CLIENT_ID), any(), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<ContractDto> result = service.getActiveContractsPageable(JOHN_DOE_CLIENT_ID, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sum Active Contracts")
    class SumActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN sumActiveContracts THEN return sum")
        void shouldReturnSumOfActiveContracts() {
            BigDecimal expectedSum = new BigDecimal("500.00");

            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID)))
                    .thenReturn(expectedSum);

            BigDecimal result = service.sumActiveContracts(JOHN_DOE_CLIENT_ID);

            assertThat(result).isEqualByComparingTo(expectedSum);
        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN sumActiveContracts THEN return zero")
        void shouldReturnZeroWhenNoActiveContracts() {
            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID)))
                    .thenReturn(BigDecimal.ZERO);
            BigDecimal result = service.sumActiveContracts(JOHN_DOE_CLIENT_ID);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("GIVEN performant endpoint requirement WHEN sumActiveContracts THEN use repository aggregation")
        void shouldUseDatabaseAggregationForPerformance() {
            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID)))
                    .thenReturn(new BigDecimal("1000.00"));
            service.sumActiveContracts(JOHN_DOE_CLIENT_ID);
            verify(contractRepository).sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID));
        }
    }

    @Nested
    @DisplayName("Close Active Contracts")
    class CloseActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN closeActiveContractsByClientId THEN close all")
        void shouldCloseAllActiveContractsForClient() {
            service.closeActiveContractsByClientId(JOHN_DOE_CLIENT_ID);
            verify(contractRepository).closeAllActiveByClientId(eq(JOHN_DOE_CLIENT_ID));
        }

        @Test
        @DisplayName("GIVEN client deletion WHEN closeActiveContractsByClientId THEN repository handles current date")
        void shouldDelegateToRepository() {
            service.closeActiveContractsByClientId(JOHN_DOE_CLIENT_ID);
            verify(contractRepository).closeAllActiveByClientId(eq(JOHN_DOE_CLIENT_ID));
        }
    }
}

