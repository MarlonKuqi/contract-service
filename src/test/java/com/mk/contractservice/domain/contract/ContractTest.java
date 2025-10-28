package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.exception.InvalidContractException;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contract - Domain Entity Tests")
class ContractTest {

    private Client testClient;

    @BeforeEach
    void setUp() {
        testClient = new Person(
                ClientName.of("John Doe"),
                Email.of("john.doe@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
    }

    @Nested
    @DisplayName("Constructor validation - Subject requirement: Contract with client, period, cost")
    class ConstructorValidation {

        @Test
        @DisplayName("GIVEN all required fields WHEN creating contract THEN contract is created with lastModified set")
        void shouldCreateContractWithAllFields() {
            OffsetDateTime startDate = OffsetDateTime.now().minusDays(10);
            OffsetDateTime endDate = OffsetDateTime.now().plusDays(20);
            ContractPeriod period = ContractPeriod.of(startDate, endDate);
            ContractCost cost = ContractCost.of(new BigDecimal("100.50"));

            Contract contract = new Contract(testClient, period, cost);

            assertThat(contract.getClient()).isEqualTo(testClient);
            assertThat(contract.getPeriod()).isEqualTo(period);
            assertThat(contract.getCostAmount()).isEqualTo(cost);
            assertThat(contract.getLastModified()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN null client WHEN creating contract THEN throw exception")
        void shouldRejectNullClient() {
            ContractPeriod period = ContractPeriod.of(OffsetDateTime.now(), OffsetDateTime.now().plusDays(30));
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> new Contract(null, period, cost))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("Client cannot be null");
        }

        @Test
        @DisplayName("GIVEN null period WHEN creating contract THEN throw exception")
        void shouldRejectNullPeriod() {
            ContractCost cost = ContractCost.of(new BigDecimal("100.00"));

            assertThatThrownBy(() -> new Contract(testClient, null, cost))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("Contract period cannot be null");
        }

        @Test
        @DisplayName("GIVEN null cost WHEN creating contract THEN throw exception")
        void shouldRejectNullCost() {
            ContractPeriod period = ContractPeriod.of(OffsetDateTime.now(), OffsetDateTime.now().plusDays(30));

            assertThatThrownBy(() -> new Contract(testClient, period, null))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("Cost amount cannot be null");
        }
    }

    @Nested
    @DisplayName("changeCost - Subject requirement: Update cost amount updates lastModified")
    class ChangeCostValidation {

        @Test
        @DisplayName("GIVEN valid new cost WHEN changing cost THEN cost is updated and lastModified is refreshed")
        void shouldUpdateCostAndLastModified() throws InterruptedException {
            ContractPeriod period = ContractPeriod.of(OffsetDateTime.now(), OffsetDateTime.now().plusDays(30));
            ContractCost initialCost = ContractCost.of(new BigDecimal("100.00"));
            Contract contract = new Contract(testClient, period, initialCost);

            OffsetDateTime initialLastModified = contract.getLastModified();
            Thread.sleep(10);

            ContractCost newCost = ContractCost.of(new BigDecimal("250.75"));
            contract.changeCost(newCost);

            assertThat(contract.getCostAmount()).isEqualTo(newCost);
            assertThat(contract.getLastModified()).isAfter(initialLastModified);
        }

        @Test
        @DisplayName("GIVEN null cost WHEN changing cost THEN throw exception")
        void shouldRejectNullCostOnUpdate() {
            ContractPeriod period = ContractPeriod.of(OffsetDateTime.now(), OffsetDateTime.now().plusDays(30));
            ContractCost initialCost = ContractCost.of(new BigDecimal("100.00"));
            Contract contract = new Contract(testClient, period, initialCost);

            assertThatThrownBy(() -> contract.changeCost(null))
                    .isInstanceOf(InvalidContractException.class)
                    .hasMessageContaining("New cost amount cannot be null");

            assertThat(contract.getCostAmount()).isEqualTo(initialCost);
        }
    }


    @Nested
    @DisplayName("ContractPeriod - Subject requirement: Start/end dates, null end date allowed")
    class ContractPeriodValidation {

        @Test
        @DisplayName("GIVEN start and end dates WHEN creating period THEN period is created")
        void shouldCreatePeriodWithBothDates() {
            OffsetDateTime start = OffsetDateTime.now();
            OffsetDateTime end = OffsetDateTime.now().plusDays(30);

            ContractPeriod period = ContractPeriod.of(start, end);

            assertThat(period.startDate()).isEqualTo(start);
            assertThat(period.endDate()).isEqualTo(end);
        }

        @Test
        @DisplayName("GIVEN null end date WHEN creating period THEN period is created with null end")
        void shouldCreatePeriodWithNullEndDate() {
            OffsetDateTime start = OffsetDateTime.now();

            ContractPeriod period = ContractPeriod.of(start, null);

            assertThat(period.startDate()).isEqualTo(start);
            assertThat(period.endDate()).isNull();
        }

        @Test
        @DisplayName("GIVEN end before start WHEN creating period THEN throw exception")
        void shouldRejectEndBeforeStart() {
            OffsetDateTime start = OffsetDateTime.now();
            OffsetDateTime end = start.minusDays(1);

            assertThatThrownBy(() -> ContractPeriod.of(start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Contract end date must be after start date");
        }
    }

    @Nested
    @DisplayName("ContractCost - Subject requirement: Cost amount with validation")
    class ContractCostValidation {

        @Test
        @DisplayName("GIVEN valid positive cost WHEN creating cost THEN cost is created")
        void shouldCreateCostWithValidAmount() {
            BigDecimal amount = new BigDecimal("1234.56");

            ContractCost cost = ContractCost.of(amount);

            assertThat(cost.value()).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("GIVEN zero cost WHEN creating cost THEN cost is created")
        void shouldCreateCostWithZeroAmount() {
            BigDecimal amount = BigDecimal.ZERO;

            ContractCost cost = ContractCost.of(amount);

            assertThat(cost.value()).isEqualByComparingTo(amount);
        }

        @Test
        @DisplayName("GIVEN negative cost WHEN creating cost THEN throw exception")
        void shouldRejectNegativeCost() {
            BigDecimal negativeAmount = new BigDecimal("-100.00");

            assertThatThrownBy(() -> ContractCost.of(negativeAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be negative");
        }

        @Test
        @DisplayName("GIVEN cost with more than 2 decimals WHEN creating cost THEN throw exception")
        void shouldRejectCostWithTooManyDecimals() {
            BigDecimal tooManyDecimals = new BigDecimal("100.123");

            assertThatThrownBy(() -> ContractCost.of(tooManyDecimals))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at most 2 decimal places");
        }

        @Test
        @DisplayName("GIVEN null amount WHEN creating cost THEN throw exception")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() -> ContractCost.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }
    }
}

