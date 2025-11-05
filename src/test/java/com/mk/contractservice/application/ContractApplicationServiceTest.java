package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private ContractApplicationService service;

    private Client testClient;
    private UUID testClientId;

    @BeforeEach
    void setUp() {
        testClientId = UUID.randomUUID();
        testClient = new Person(
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
    }

    @Nested
    @DisplayName("Create Contract")
    class CreateContractTests {

        @Test
        @DisplayName("GIVEN valid client and contract data WHEN createForClient THEN contract is created")
        void shouldCreateContractForClient() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(30);
            BigDecimal amount = new BigDecimal("100.50");

            when(clientRepository.findById(testClientId)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(testClientId, start, end, amount);

            assertThat(result).isNotNull();
            assertThat(result.getClient()).isEqualTo(testClient);
            assertThat(result.getPeriod().startDate()).isEqualTo(start);
            assertThat(result.getPeriod().endDate()).isEqualTo(end);
            assertThat(result.getCostAmount().value()).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("GIVEN null start date WHEN createForClient THEN use current date")
        void shouldUseCurrentDateWhenStartIsNull() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            LocalDateTime end = LocalDateTime.now().plusDays(30);
            BigDecimal amount = new BigDecimal("100.00");

            when(clientRepository.findById(testClientId)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(testClientId, null, end, amount);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(result.getPeriod().startDate()).isNotNull();
            assertThat(result.getPeriod().startDate()).isBetween(before, after);
        }

        @Test
        @DisplayName("GIVEN null end date WHEN createForClient THEN create with null end")
        void shouldAcceptNullEndDate() {
            LocalDateTime start = LocalDateTime.now();
            BigDecimal amount = new BigDecimal("100.00");

            when(clientRepository.findById(testClientId)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(testClientId, start, null, amount);

            assertThat(result.getPeriod().endDate()).isNull();
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN createForClient THEN throw ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createForClient(nonExistentId, LocalDateTime.now(), null, BigDecimal.TEN))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("Client not found");

            verify(contractRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN valid data WHEN createForClient THEN contract is saved")
        void shouldSaveContractToRepository() {
            LocalDateTime start = LocalDateTime.now();
            BigDecimal amount = new BigDecimal("200.00");

            when(clientRepository.findById(testClientId)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.createForClient(testClientId, start, null, amount);

            ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(captor.capture());

            Contract savedContract = captor.getValue();
            assertThat(savedContract.getClient()).isEqualTo(testClient);
            assertThat(savedContract.getCostAmount().value()).isEqualByComparingTo(amount);
        }
    }

    @Nested
    @DisplayName("Update Contract Cost")
    class UpdateCostTests {

        @Test
        @DisplayName("GIVEN existing contract WHEN updateCost THEN cost is updated")
        void shouldUpdateCostForExistingContract() {
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            Contract contract = new Contract(
                    testClient,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("100.00"))
            );
            BigDecimal newAmount = new BigDecimal("200.00");

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            boolean result = service.updateCost(clientId, contractId, newAmount);

            assertThat(result).isTrue();
            assertThat(contract.getCostAmount().value()).isEqualByComparingTo(newAmount);
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN updateCost THEN return false")
        void shouldReturnFalseWhenContractNotFound() {
            UUID clientId = UUID.randomUUID();
            UUID nonExistentId = UUID.randomUUID();
            when(contractRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            boolean result = service.updateCost(clientId, nonExistentId, BigDecimal.TEN);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("GIVEN valid new cost WHEN updateCost THEN lastModified is updated")
        void shouldUpdateLastModifiedWhenCostChanged() throws InterruptedException {
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            Contract contract = new Contract(
                    testClient,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("100.00"))
            );
            LocalDateTime initialLastModified = contract.getLastModified();
            Thread.sleep(10);

            when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

            service.updateCost(clientId, contractId, new BigDecimal("250.00"));

            assertThat(contract.getLastModified()).isAfter(initialLastModified);
        }
    }

    @Nested
    @DisplayName("Get Active Contracts")
    class GetActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN getActiveContractsPageable THEN return paginated active contracts")
        void shouldReturnActiveContracts() {
            LocalDateTime updatedSince = LocalDateTime.now().minusDays(7);
            Contract contract = new Contract(testClient, ContractPeriod.of(LocalDateTime.now(), null), ContractCost.of(BigDecimal.TEN));
            Page<Contract> expectedPage = new org.springframework.data.domain.PageImpl<>(List.of(contract));
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(testClientId), any(LocalDateTime.class), eq(updatedSince), eq(pageable)))
                    .thenReturn(expectedPage);

            Page<Contract> result = service.getActiveContractsPageable(testClientId, updatedSince, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(contract);

        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN getActiveContractsPageable THEN return empty page")
        void shouldReturnEmptyPageWhenNoActiveContracts() {
            Page<Contract> emptyPage = org.springframework.data.domain.Page.empty();
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(testClientId), any(LocalDateTime.class), any(), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<Contract> result = service.getActiveContractsPageable(testClientId, null, pageable);

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

            when(contractRepository.sumActiveByClientId(eq(testClientId), any(LocalDateTime.class)))
                    .thenReturn(expectedSum);

            BigDecimal result = service.sumActiveContracts(testClientId);

            assertThat(result).isEqualByComparingTo(expectedSum);
        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN sumActiveContracts THEN return zero")
        void shouldReturnZeroWhenNoActiveContracts() {
            when(contractRepository.sumActiveByClientId(eq(testClientId), any(LocalDateTime.class)))
                    .thenReturn(BigDecimal.ZERO);
            BigDecimal result = service.sumActiveContracts(testClientId);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("GIVEN performant endpoint requirement WHEN sumActiveContracts THEN use repository aggregation")
        void shouldUseDatabaseAggregationForPerformance() {
            when(contractRepository.sumActiveByClientId(eq(testClientId), any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("1000.00"));
            service.sumActiveContracts(testClientId);
            verify(contractRepository).sumActiveByClientId(eq(testClientId), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Close Active Contracts")
    class CloseActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN closeActiveContractsByClientId THEN close all")
        void shouldCloseAllActiveContractsForClient() {
            service.closeActiveContractsByClientId(testClientId);
            verify(contractRepository).closeAllActiveByClientId(eq(testClientId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("GIVEN client deletion WHEN closeActiveContractsByClientId THEN use current date as end date")
        void shouldUseCurrentDateAsEndDate() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            service.closeActiveContractsByClientId(testClientId);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(contractRepository).closeAllActiveByClientId(eq(testClientId), captor.capture());

            LocalDateTime usedDate = captor.getValue();
            assertThat(usedDate).isBetween(before, after);
        }
    }
}



