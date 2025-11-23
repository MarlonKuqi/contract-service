package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractService - Business Logic Tests")
class ContractServiceTest {

    private ContractService service;

    private Client testClient;

    @BeforeEach
    void setUp() {
        service = new ContractService();

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
}
