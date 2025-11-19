package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractService;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ContractApplicationService service;

    private static final UUID JOHN_DOE_CLIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = Person.reconstitute(
                JOHN_DOE_CLIENT_ID,
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

            when(clientRepository.findById(JOHN_DOE_CLIENT_ID)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(JOHN_DOE_CLIENT_ID, start, end, amount);

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

            when(clientRepository.findById(JOHN_DOE_CLIENT_ID)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(JOHN_DOE_CLIENT_ID, null, end, amount);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(result.getPeriod().startDate()).isNotNull();
            assertThat(result.getPeriod().startDate()).isBetween(before, after);
        }

        @Test
        @DisplayName("GIVEN null end date WHEN createForClient THEN create with null end")
        void shouldAcceptNullEndDate() {
            LocalDateTime start = LocalDateTime.now();
            BigDecimal amount = new BigDecimal("100.00");

            when(clientRepository.findById(JOHN_DOE_CLIENT_ID)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Contract result = service.createForClient(JOHN_DOE_CLIENT_ID, start, null, amount);

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

            when(clientRepository.findById(JOHN_DOE_CLIENT_ID)).thenReturn(Optional.of(testClient));
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.createForClient(JOHN_DOE_CLIENT_ID, start, null, amount);

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
            UUID contractId = UUID.randomUUID();
            Contract contract = Contract.builder()
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(new BigDecimal("100.00")))
                    .build();
            BigDecimal newAmount = new BigDecimal("200.00");

            when(contractService.getContractForClient(JOHN_DOE_CLIENT_ID, contractId))
                    .thenReturn(contract);

            service.updateCost(JOHN_DOE_CLIENT_ID, contractId, newAmount);

            assertThat(contract.getCostAmount().value()).isEqualByComparingTo(newAmount);
            verify(contractRepository).save(contract);
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN updateCost THEN throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            UUID clientId = UUID.randomUUID();
            UUID nonExistentId = UUID.randomUUID();
            when(contractService.getContractForClient(clientId, nonExistentId))
                    .thenThrow(new ContractNotFoundException(nonExistentId));

            assertThatThrownBy(() -> service.updateCost(clientId, nonExistentId, BigDecimal.TEN))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(nonExistentId.toString());
        }


        @Test
        @DisplayName("GIVEN contract belonging to different client WHEN updateCost THEN throw ContractNotOwnedByClientException")
        void shouldThrowExceptionWhenContractBelongsToAnotherClient() {
            UUID contractId = UUID.randomUUID();
            UUID differentClientId = UUID.randomUUID();

            when(contractService.getContractForClient(differentClientId, contractId))
                    .thenThrow(new ContractNotOwnedByClientException(contractId, differentClientId));

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
            Contract expiredContract = Contract.builder()
                    .id(contractId)
                    .client(testClient)
                    .period(ContractPeriod.of(now.minusDays(100), now.minusDays(1)))
                    .costAmount(ContractCost.of(new BigDecimal("100.00")))
                    .build();

            when(contractService.getContractForClient(JOHN_DOE_CLIENT_ID, contractId))
                    .thenReturn(expiredContract);

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
            Contract contract = Contract.builder()
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(BigDecimal.valueOf(1000)))
                    .build();

            when(contractService.getContractForClient(JOHN_DOE_CLIENT_ID, contractId))
                    .thenReturn(contract);

            Contract result = service.getContractById(JOHN_DOE_CLIENT_ID, contractId);

            assertThat(result).isEqualTo(contract);
            verify(contractService).getContractForClient(JOHN_DOE_CLIENT_ID, contractId);
        }

        @Test
        @DisplayName("GIVEN non-existent contract WHEN getContractById THEN throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            UUID contractId = UUID.randomUUID();

            when(contractService.getContractForClient(JOHN_DOE_CLIENT_ID, contractId))
                    .thenThrow(new ContractNotFoundException(contractId));

            assertThatThrownBy(() -> service.getContractById(JOHN_DOE_CLIENT_ID, contractId))
                    .isInstanceOf(ContractNotFoundException.class)
                    .hasMessageContaining(contractId.toString());
        }

        @Test
        @DisplayName("GIVEN contract belonging to different client WHEN getContractById THEN throw ContractNotOwnedByClientException")
        void shouldThrowExceptionWhenContractBelongsToDifferentClient() {
            UUID contractId = UUID.randomUUID();
            UUID differentClientId = UUID.randomUUID();

            when(contractService.getContractForClient(differentClientId, contractId))
                    .thenThrow(new ContractNotOwnedByClientException(contractId, differentClientId));

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
            Contract contract = Contract.builder()
                    .client(testClient)
                    .period(ContractPeriod.of(LocalDateTime.now(), null))
                    .costAmount(ContractCost.of(BigDecimal.TEN))
                    .build();
            Page<Contract> expectedPage = new PageImpl<>(List.of(contract));
            Pageable pageable = PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class), eq(updatedSince), eq(pageable)))
                    .thenReturn(expectedPage);

            Page<Contract> result = service.getActiveContractsPageable(JOHN_DOE_CLIENT_ID, updatedSince, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(contract);

        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN getActiveContractsPageable THEN return empty page")
        void shouldReturnEmptyPageWhenNoActiveContracts() {
            Page<Contract> emptyPage = Page.empty();
            Pageable pageable = PageRequest.of(0, 20);

            when(contractRepository.findActiveByClientIdPageable(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class), any(), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<Contract> result = service.getActiveContractsPageable(JOHN_DOE_CLIENT_ID, null, pageable);

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

            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class)))
                    .thenReturn(expectedSum);

            BigDecimal result = service.sumActiveContracts(JOHN_DOE_CLIENT_ID);

            assertThat(result).isEqualByComparingTo(expectedSum);
        }

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN sumActiveContracts THEN return zero")
        void shouldReturnZeroWhenNoActiveContracts() {
            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class)))
                    .thenReturn(BigDecimal.ZERO);
            BigDecimal result = service.sumActiveContracts(JOHN_DOE_CLIENT_ID);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("GIVEN performant endpoint requirement WHEN sumActiveContracts THEN use repository aggregation")
        void shouldUseDatabaseAggregationForPerformance() {
            when(contractRepository.sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("1000.00"));
            service.sumActiveContracts(JOHN_DOE_CLIENT_ID);
            verify(contractRepository).sumActiveByClientId(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Close Active Contracts")
    class CloseActiveContractsTests {

        @Test
        @DisplayName("GIVEN client with active contracts WHEN closeActiveContractsByClientId THEN close all")
        void shouldCloseAllActiveContractsForClient() {
            service.closeActiveContractsByClientId(JOHN_DOE_CLIENT_ID);
            verify(contractRepository).closeAllActiveByClientId(eq(JOHN_DOE_CLIENT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("GIVEN client deletion WHEN closeActiveContractsByClientId THEN use current date as end date")
        void shouldUseCurrentDateAsEndDate() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            service.closeActiveContractsByClientId(JOHN_DOE_CLIENT_ID);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(contractRepository).closeAllActiveByClientId(eq(JOHN_DOE_CLIENT_ID), captor.capture());

            LocalDateTime usedDate = captor.getValue();
            assertThat(usedDate).isBetween(before, after);
        }
    }
}



