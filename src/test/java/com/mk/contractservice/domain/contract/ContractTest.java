package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contract - Domain Entity Tests")
class ContractTest {

    private UUID testClientId;

    @BeforeEach
    void setUp() {
        testClientId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Constructor validation - Subject requirement: Contract with client, period, cost")
    class ConstructorValidation {

        @Test
        @DisplayName("GIVEN all required fields WHEN creating contract THEN contract is created")
        void shouldCreateContractWithAllFields() {
            LocalDateTime startDate = LocalDateTime.now().minusDays(10);
            LocalDateTime endDate = LocalDateTime.now().plusDays(20);
            ContractPeriod period = ContractPeriod.of(startDate, endDate);
            ContractCost cost = ContractCost.of(new BigDecimal("100.50"));

            Contract contract = Contract.of(testClientId, period, cost);

            assertThat(contract.getClientId()).isEqualTo(testClientId);
            assertThat(contract.getPeriod()).isEqualTo(period);
            assertThat(contract.getCostAmount()).isEqualTo(cost);
        }

        @Test
        @DisplayName("GIVEN null client WHEN creating contract THEN throw exception")
        void shouldRejectNullClient() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> Contract.of(null, period, cost))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("Client cannot be null");
        }

        @Test
        @DisplayName("GIVEN null period WHEN creating contract THEN throw exception")
        void shouldRejectNullPeriod() {
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> Contract.of(testClientId, null, cost))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("Contract period cannot be null");
        }

        @Test
        @DisplayName("GIVEN null cost WHEN creating contract THEN throw exception")
        void shouldRejectNullCost() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));

            assertThatThrownBy(() -> Contract.of(testClientId, period, null))
                    .isInstanceOf(InvalidContractException.class);
        }
    }

    @Nested
    @DisplayName("changeCost - Update cost amount")
    class ChangeCostValidation {

        @Test
        @DisplayName("GIVEN valid new cost WHEN changing cost THEN cost is updated")
        void shouldUpdateCost() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            ContractCost initialCost = ContractCost.of(new BigDecimal("100.00"));
            Contract contract = Contract.of(testClientId, period, initialCost);

            BigDecimal newCostAmount = new BigDecimal("250.75");
            Contract updatedContract = contract.changeCost(newCostAmount);

            assertThat(updatedContract.getCostAmount()).isEqualTo(ContractCost.of(newCostAmount));
            assertThat(contract.getCostAmount()).isEqualTo(initialCost);
        }

        @Test
        @DisplayName("GIVEN null cost WHEN changing cost THEN throw exception")
        void shouldRejectNullCostOnUpdate() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            ContractCost initialCost = ContractCost.of(new BigDecimal("100.00"));
            Contract contract = Contract.of(testClientId, period, initialCost);

            assertThatThrownBy(() -> contract.changeCost(null))
                    .isInstanceOf(InvalidContractCostException.class);

            assertThat(contract.getCostAmount()).isEqualTo(initialCost);
        }

        @Test
        @DisplayName("GIVEN expired contract WHEN changing cost THEN throw ExpiredContractException")
        void shouldRejectCostChangeOnExpiredContract() {
            UUID contractId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            ContractPeriod expiredPeriod = ContractPeriod.of(now.minusDays(100), now.minusDays(1));
            ContractCost initialCost = ContractCost.of(new BigDecimal("100.00"));

            Contract contract = Contract.reconstituteFromDatabase(contractId, testClientId, expiredPeriod, initialCost);
            BigDecimal newCost = new BigDecimal("200.00");

            assertThatThrownBy(() -> contract.changeCost(newCost))
                    .isInstanceOf(ExpiredContractException.class)
                    .hasMessageContaining(contractId.toString());

            assertThat(contract.getCostAmount()).isEqualTo(initialCost);
        }
    }

    @Nested
    @DisplayName("isActive - Subject requirement: Determine if contract is currently active")
    class IsActiveValidation {

        @Test
        @DisplayName("GIVEN contract with null getEndDate WHEN checking isActive THEN should be active")
        void shouldBeActiveWhenEndDateIsNull() {
            LocalDateTime now = LocalDateTime.now();
            ContractPeriod period = ContractPeriod.of(now.minusDays(10), null);
            Contract contract = Contract.of(testClientId, period, ContractCost.of(BigDecimal.valueOf(1000)));

            assertThat(contract.isActive()).isTrue();
        }

        @Test
        @DisplayName("GIVEN contract with future getEndDate WHEN checking isActive THEN should be active")
        void shouldBeActiveWhenEndDateIsInFuture() {
            LocalDateTime now = LocalDateTime.now();
            ContractPeriod period = ContractPeriod.of(now.minusDays(10), now.plusDays(30));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(BigDecimal.valueOf(1000)));

            assertThat(contract.isActive()).isTrue();
        }

        @Test
        @DisplayName("GIVEN contract with past getEndDate WHEN checking isActive THEN should NOT be active")
        void shouldNotBeActiveWhenEndDateIsInPast() {
            LocalDateTime now = LocalDateTime.now();
            ContractPeriod period = ContractPeriod.of(now.minusDays(100), now.minusDays(1));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(BigDecimal.valueOf(1000)));

            assertThat(contract.isActive()).isFalse();
        }
    }


    @Nested
    @DisplayName("ContractPeriod validation via Contract.of()")
    class ContractPeriodValidation {

        @Test
        @DisplayName("GIVEN valid period WHEN creating contract THEN contract is created")
        void shouldCreateContractWithValidPeriod() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(30);
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            Contract contract = Contract.of(testClientId, ContractPeriod.of(start, end), cost);

            assertThat(contract.getPeriod().getStartDate()).isEqualTo(start);
            assertThat(contract.getPeriod().getEndDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("GIVEN null end date WHEN creating contract THEN contract is created with open-ended period")
        void shouldCreateContractWithOpenEndedPeriod() {
            LocalDateTime start = LocalDateTime.now();
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            Contract contract = Contract.of(testClientId, ContractPeriod.of(start, null), cost);

            assertThat(contract.getPeriod().getStartDate()).isEqualTo(start);
            assertThat(contract.getPeriod().getEndDate()).isNull();
        }

        @Test
        @DisplayName("GIVEN end before start WHEN creating contract THEN throw InvalidContractPeriodException")
        void shouldRejectContractWithInvalidPeriod() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.minusDays(1);
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> Contract.of(testClientId, ContractPeriod.of(start, end), cost))
                    .isInstanceOf(InvalidContractPeriodException.class)
                    .hasMessageContaining("Contract end date must be after start date");
        }

        @Test
        @DisplayName("GIVEN end equal to start WHEN creating contract THEN throw InvalidContractPeriodException")
        void shouldRejectContractWithEndEqualToStart() {
            LocalDateTime start = LocalDateTime.now();
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> Contract.of(testClientId, ContractPeriod.of(start, start), cost))
                    .isInstanceOf(InvalidContractPeriodException.class)
                    .hasMessageContaining("Contract end date must be after start date");
        }
    }

    @Nested
    @DisplayName("ContractCost validation via Contract.of() and changeCost()")
    class ContractCostValidation {

        @Test
        @DisplayName("GIVEN valid positive cost WHEN creating contract THEN contract is created")
        void shouldCreateContractWithValidCost() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            BigDecimal amount = new BigDecimal("1234.56");

            Contract contract = Contract.of(testClientId, period, ContractCost.of(amount));

            assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("GIVEN zero cost WHEN creating contract THEN throw InvalidContractCostException")
        void shouldRejectContractWithZeroCost() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));

            assertThatThrownBy(() -> Contract.of(testClientId, period, ContractCost.of(BigDecimal.ZERO)))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("must be greater than zero");
        }

        @Test
        @DisplayName("GIVEN negative cost WHEN creating contract THEN throw InvalidContractCostException")
        void shouldRejectContractWithNegativeCost() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));

            assertThatThrownBy(() -> Contract.of(testClientId, period, ContractCost.of(new BigDecimal("-100.00"))))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("must be greater than zero");
        }

        @Test
        @DisplayName("GIVEN cost with more than 2 decimals WHEN creating contract THEN throw InvalidContractCostException")
        void shouldRejectContractWithTooManyDecimals() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));

            assertThatThrownBy(() -> Contract.of(testClientId, period, ContractCost.of(new BigDecimal("100.123"))))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("at most 2 decimal places");
        }

        @Test
        @DisplayName("GIVEN null cost amount WHEN changing cost THEN throw InvalidContractCostException")
        void shouldRejectChangeCostWithNull() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(new BigDecimal("100.00")));

            assertThatThrownBy(() -> contract.changeCost(null))
                    .isInstanceOf(InvalidContractCostException.class);
        }

        @Test
        @DisplayName("GIVEN zero amount WHEN changing cost THEN throw InvalidContractCostException")
        void shouldRejectChangeCostWithZero() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(new BigDecimal("100.00")));

            assertThatThrownBy(() -> contract.changeCost(BigDecimal.ZERO))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("must be greater than zero");
        }

        @Test
        @DisplayName("GIVEN negative amount WHEN changing cost THEN throw InvalidContractCostException")
        void shouldRejectChangeCostWithNegative() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(new BigDecimal("100.00")));

            assertThatThrownBy(() -> contract.changeCost(new BigDecimal("-50.00")))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("must be greater than zero");
        }

        @Test
        @DisplayName("GIVEN amount with too many decimals WHEN changing cost THEN throw InvalidContractCostException")
        void shouldRejectChangeCostWithTooManyDecimals() {
            ContractPeriod period = ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusDays(30));
            Contract contract = Contract.of(testClientId, period, ContractCost.of(new BigDecimal("100.00")));

            assertThatThrownBy(() -> contract.changeCost(new BigDecimal("200.999")))
                    .isInstanceOf(InvalidContractCostException.class)
                    .hasMessageContaining("at most 2 decimal places");
        }
    }
}



