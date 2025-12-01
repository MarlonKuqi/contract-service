package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService - Business Logic Tests")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractService service;

    private Client testClient;

    @BeforeEach
    void setUp() {

        testClient = Person.reconstitute(
                UUID.randomUUID(),
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 15)));
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
                    testClient,
                    ContractPeriod.of(LocalDateTime.now(), null),
                    ContractCost.of(new BigDecimal("1000.00")));

            service.ensureContractBelongsToClient(contract, testClient.getId());
            assertThat(contract.getClient().getId()).isEqualTo(testClient.getId());
        }

        @Test
        @DisplayName("Should throw when contract belongs to different client")
        void shouldThrowWhenContractBelongsToDifferentClient() {
            UUID differentClientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();

            Contract contract = Contract.reconstitute(
                    contractId,
                    testClient,
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
        @DisplayName("Should create contract with all provided data")
        void shouldCreateContract() {
            LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
            ContractPeriod period = ContractPeriod.of(start, end);
            ContractCost cost = ContractCost.of(new BigDecimal("1500.00"));

            Contract result = Contract.of(testClient, period, cost);

            assertThat(result).isNotNull();
            assertThat(result.getClient()).isEqualTo(testClient);
            assertThat(result.getPeriod()).isEqualTo(period);
            assertThat(result.getCostAmount()).isEqualTo(cost);
            assertThat(result.getId()).isNull(); // Not persisted yet
        }

        @Test
        @DisplayName("Should create contract with open-ended period")
        void shouldCreateContractWithOpenEndedPeriod() {
            LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
            ContractPeriod period = ContractPeriod.of(start, null);
            ContractCost cost = ContractCost.of(new BigDecimal("2000.00"));

            Contract result = Contract.of(testClient, period, cost);

            assertThat(result).isNotNull();
            assertThat(result.getPeriod().endDate()).isNull();
        }
    }

    @Nested
    @DisplayName("Ensure Contract Is Active - Business Rule")
    class EnsureContractIsActiveTests {

        @Test
        @DisplayName("GIVEN active contract WHEN ensureContractIsActive THEN no exception thrown")
        void shouldNotThrowWhenContractIsActive() {
            UUID contractId = UUID.randomUUID();
            Contract activeContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(LocalDateTime.now().minusDays(10), null),
                    ContractCost.of(new BigDecimal("1000.00")));

            service.ensureContractIsActive(activeContract);
        }

        @Test
        @DisplayName("GIVEN expired contract WHEN ensureContractIsActive THEN throw ExpiredContractException")
        void shouldThrowExceptionWhenContractIsInactive() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Contract expiredContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(now.minusDays(100), now.minusDays(1)),
                    ContractCost.of(new BigDecimal("1000.00")));

            assertThatThrownBy(() -> service.ensureContractIsActive(expiredContract))
                    .isInstanceOf(ExpiredContractException.class)
                    .hasMessageContaining(contractId.toString());
        }

        @Test
        @DisplayName("GIVEN contract with future end date WHEN ensureContractIsActive THEN no exception thrown")
        void shouldNotThrowWhenContractHasFutureEndDate() {
            UUID contractId = UUID.randomUUID();
            Contract futureContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(365)),
                    ContractCost.of(new BigDecimal("1000.00")));

            service.ensureContractIsActive(futureContract);
        }

        @Test
        @DisplayName("GIVEN contract with null end date WHEN ensureContractIsActive THEN no exception thrown")
        void shouldNotThrowWhenContractHasOpenEndedPeriod() {
            UUID contractId = UUID.randomUUID();
            Contract openEndedContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(LocalDateTime.now().minusDays(30), null),
                    ContractCost.of(new BigDecimal("1000.00")));

            service.ensureContractIsActive(openEndedContract);
        }
    }

    @Nested
    @DisplayName("Close Contract - Domain Business Logic")
    class CloseContractTests {

        @Test
        @DisplayName("GIVEN active contract WHEN closeContract THEN returns new instance with closed period")
        void shouldReturnNewInstanceWithClosedPeriod() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            Contract activeContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(startDate, null),
                    ContractCost.of(new BigDecimal("1000.00")));

            Contract closedContract = service.closeContract(activeContract);

            assertThat(closedContract).isNotNull();
            assertThat(closedContract.getId()).isEqualTo(contractId);
            assertThat(closedContract.isInactive()).isTrue();
            assertThat(closedContract.getPeriod().endDate()).isNotNull();
            assertThat(closedContract.getPeriod().startDate()).isEqualTo(startDate);
            assertThat(activeContract.isActive()).isTrue();
        }

        @Test
        @DisplayName("GIVEN expired contract WHEN closeContract THEN throw ExpiredContractException")
        void shouldThrowExceptionWhenContractAlreadyExpired() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Contract expiredContract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(now.minusDays(100), now.minusDays(1)),
                    ContractCost.of(new BigDecimal("1000.00")));

            assertThatThrownBy(() -> service.closeContract(expiredContract))
                    .isInstanceOf(ExpiredContractException.class)
                    .hasMessageContaining(contractId.toString());
        }

        @Test
        @DisplayName("GIVEN contract with future end date WHEN closeContract THEN closes early")
        void shouldCloseContractEarlyWithFutureEndDate() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime futureEndDate = LocalDateTime.now().plusDays(365);
            Contract contract = Contract.reconstitute(
                    contractId,
                    testClient,
                    ContractPeriod.of(LocalDateTime.now().minusDays(30), futureEndDate),
                    ContractCost.of(new BigDecimal("1000.00")));

            Contract closedContract = service.closeContract(contract);

            assertThat(closedContract.getPeriod().endDate()).isNotNull();
            assertThat(closedContract.getPeriod().endDate()).isBefore(futureEndDate);
            assertThat(closedContract.isInactive()).isTrue();
        }
    }
}
