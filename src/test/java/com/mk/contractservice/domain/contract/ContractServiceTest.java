package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService - Business Logic Tests")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    private ContractService service;

    private UUID testClientId;

    @BeforeEach
    void setUp() {
        service = new ContractService(contractRepository);
        testClientId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Ensure Contract Belongs To Client - Business Rule")
    class EnsureContractBelongsToClientTests {

        @Test
        @DisplayName("Should not throw when contract belongs to client")
        void shouldNotThrowWhenContractBelongsToClient() {
            UUID contractId = UUID.randomUUID();

            Contract contract = Contract.reconstitute(
                    contractId,
                    testClientId,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("1000.00")));

            service.ensureContractBelongsToClient(contract, testClientId);
            assertThat(contract.getClientId()).isEqualTo(testClientId);
        }

        @Test
        @DisplayName("Should throw when contract belongs to different client")
        void shouldThrowWhenContractBelongsToDifferentClient() {
            UUID differentClientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Contract contract = Contract.reconstitute(
                    contractId,
                    testClientId,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("1000.00")));

            assertThatThrownBy(() -> service.ensureContractBelongsToClient(contract, differentClientId))
                    .isInstanceOf(ContractNotOwnedByClientException.class)
                    .hasMessageContaining(contractId.toString())
                    .hasMessageContaining(differentClientId.toString());
        }
    }

    @Nested
    @DisplayName("Create Contract - Domain Logic")
    class CreateContractTests {

        @Test
        @DisplayName("Should create and persist contract with all provided data")
        void shouldCreateAndPersistContract() {
            LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
            ContractPeriod period = ContractPeriod.of(start, end);
            ContractCost cost = ContractCost.of(new BigDecimal("1500.00"));

            UUID savedContractId = UUID.randomUUID();
            Contract expectedSavedContract = Contract.reconstitute(
                    savedContractId,
                    testClientId,
                    period,
                    cost
            );

            when(contractRepository.save(any(Contract.class))).thenReturn(expectedSavedContract);

            Contract result = service.createAndPersistContract(testClientId, period, cost);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedContractId);
            assertThat(result.getClientId()).isEqualTo(testClientId);
            assertThat(result.getPeriod()).isEqualTo(period);
            assertThat(result.getCostAmount()).isEqualTo(cost);

            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("Should create and persist contract with open-ended period (null end date)")
        void shouldCreateAndPersistContractWithOpenEndedPeriod() {
            LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
            ContractPeriod period = ContractPeriod.of(start, null);
            ContractCost cost = ContractCost.of(new BigDecimal("2000.00"));

            UUID savedContractId = UUID.randomUUID();
            Contract expectedSavedContract = Contract.reconstitute(
                    savedContractId,
                    testClientId,
                    period,
                    cost
            );

            when(contractRepository.save(any(Contract.class))).thenReturn(expectedSavedContract);

            Contract result = service.createAndPersistContract(testClientId, period, cost);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedContractId);
            assertThat(result.getPeriod().endDate()).isNull();
            assertThat(result.isActive()).isTrue(); // Open-ended contract is active

            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("Should delegate persistence to repository")
        void shouldDelegatePersistenceToRepository() {
            LocalDateTime start = LocalDateTime.now();
            ContractPeriod period = ContractPeriod.of(start, null);
            ContractCost cost = ContractCost.of(new BigDecimal("1000.00"));

            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
                Contract contract = invocation.getArgument(0);
                return Contract.reconstitute(
                        UUID.randomUUID(),
                        contract.getClientId(),
                        contract.getPeriod(),
                        contract.getCostAmount()
                );
            });

            service.createAndPersistContract(testClientId, period, cost);

            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("Should preserve contract business rules during creation")
        void shouldPreserveContractBusinessRules() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(30);
            ContractPeriod period = ContractPeriod.of(start, end);
            ContractCost cost = ContractCost.of(new BigDecimal("500.00"));

            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
                Contract contract = invocation.getArgument(0);
                return Contract.reconstitute(
                        UUID.randomUUID(),
                        contract.getClientId(),
                        contract.getPeriod(),
                        contract.getCostAmount()
                );
            });

            Contract result = service.createAndPersistContract(testClientId, period, cost);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getCostAmount().value()).isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }
}
