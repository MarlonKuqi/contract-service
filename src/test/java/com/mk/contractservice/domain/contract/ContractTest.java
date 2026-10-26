package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contract - Domain Entity Tests")
class ContractTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);

    private UUID testClientId;

    @BeforeEach
    void setUp() {
        testClientId = UUID.randomUUID();
    }

    static Stream<BigDecimal> invalidCosts() {
        return Stream.of(
                null,
                BigDecimal.ZERO,
                new BigDecimal("-100.00"),
                new BigDecimal("-0.01"),
                new BigDecimal("100.123"),  // Too many decimals
                new BigDecimal("50.9999"),  // Too many decimals
                new BigDecimal("10000000000.00")  // Too many integer digits
        );
    }

    static Stream<Object[]> validPeriods() {
        LocalDateTime now = NOW;
        return Stream.of(
                new Object[]{now, null},
                new Object[]{null, now.plusDays(30)},
                new Object[]{null, null},
                new Object[]{now.minusDays(10), now.plusDays(30)},
                new Object[]{now, now.plusDays(1)},
                new Object[]{now.minusMonths(1), now.plusMonths(6)},
                new Object[]{LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 23, 59)},
                new Object[]{now.minusYears(1), null}
        );
    }

    static Stream<Object[]> invalidPeriods() {
        LocalDateTime now = NOW;
        return Stream.of(
                new Object[]{now, now.minusDays(1)},                     // End before start
                new Object[]{now, now},                                  // End equals start
                new Object[]{now.plusDays(10), now.plusDays(5)},        // End before start (future dates)
                new Object[]{LocalDateTime.of(2024, 1, 15, 10, 0), LocalDateTime.of(2024, 1, 10, 10, 0)},  // End before start (specific dates)
                new Object[]{LocalDateTime.of(2024, 6, 1, 0, 0), LocalDateTime.of(2024, 6, 1, 0, 0)}      // Start equals end (same instant)
        );
    }

    static Stream<Object[]> activeContractPeriods() {
        LocalDateTime now = NOW;
        return Stream.of(
                new Object[]{now.minusDays(1), now.plusDays(1)},
                new Object[]{now.minusMonths(1), now.plusMonths(1)},
                new Object[]{now.minusDays(365), now.plusDays(365)},
                new Object[]{now.minusHours(1), null},  // Open-ended
                new Object[]{now.plusDays(1), now.plusDays(2)},
                new Object[]{now.plusMonths(1), now.plusMonths(2)},
                new Object[]{now.plusHours(1), null}
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Nested
        @DisplayName("From Command")
        class FromCommand {

            @ParameterizedTest
            @CsvSource({
                    "100.00, 30",
                    "1234.56, 365",
                    "0.01, 1",
                    "99999.99, 1000",
                    "50.50, 90",
                    "9999999999.99, 1"
            })
            @DisplayName("GIVEN various valid combinations WHEN creating Contract THEN contract is created with all correct values")
            void shouldCreateContractWithValidCombinations(BigDecimal costAmount, long daysInFuture) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(daysInFuture);

                Contract contract = ContractFactory.createFromCommand(testClientId, startDate, endDate, costAmount, NOW);

                assertThat(contract.getClientId()).isEqualTo(testClientId);
                assertThat(contract.getPeriod().getStartDate()).isEqualTo(startDate);
                assertThat(contract.getPeriod().getEndDate()).isEqualTo(endDate);
                assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#validPeriods")
            @DisplayName("GIVEN various valid periods WHEN creating Contract THEN contract is created with correct dates")
            void shouldCreateContractWithValidPeriods(LocalDateTime startDate, LocalDateTime endDate) {
                BigDecimal costAmount = new BigDecimal("500.00");

                Contract contract = ContractFactory.createFromCommand(testClientId, startDate, endDate, costAmount, NOW);

                assertThat(contract.getPeriod().getStartDate()).isNotNull();
                if (startDate == null) {
                    assertThat(contract.getPeriod().getStartDate()).isBeforeOrEqualTo(NOW);
                } else {
                    assertThat(contract.getPeriod().getStartDate()).isEqualTo(startDate);
                }
                assertThat(contract.getPeriod().getEndDate()).isEqualTo(endDate);
                assertThat(contract.getClientId()).isEqualTo(testClientId);
                assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidCosts")
            @DisplayName("GIVEN invalid cost WHEN creating Contract THEN throw InvalidContractCostException")
            void shouldRejectInvalidCost(BigDecimal invalidCost) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);

                assertThatThrownBy(() -> ContractFactory.createFromCommand(testClientId, startDate, endDate, invalidCost, NOW))
                        .isInstanceOf(InvalidContractCostException.class);
            }

            @ParameterizedTest
            @CsvSource({
                    "-0.001, decimal places",
                    "-99999999999999, integer digits"
            })
            @DisplayName("GIVEN negative cost with an invalid precision WHEN creating Contract THEN the precision error is reported first")
            void shouldReportPrecisionBeforePositivity(BigDecimal invalidCost, String expectedMessage) {
                assertThatThrownBy(() -> ContractFactory.createFromCommand(testClientId, NOW, NOW.plusDays(30), invalidCost, NOW))
                        .isInstanceOf(InvalidContractCostException.class)
                        .hasMessageContaining(expectedMessage);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidPeriods")
            @DisplayName("GIVEN invalid period WHEN creating Contract THEN throw InvalidContractPeriodException")
            void shouldRejectInvalidPeriod(LocalDateTime startDate, LocalDateTime endDate) {
                BigDecimal costAmount = new BigDecimal("100.00");

                assertThatThrownBy(() -> ContractFactory.createFromCommand(testClientId, startDate, endDate, costAmount, NOW))
                        .isInstanceOf(InvalidContractPeriodException.class);
            }


            @Test
            @DisplayName("GIVEN null client ID WHEN creating Contract THEN throw InvalidDomainObjectError")
            void shouldRejectNullClientId() {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);
                BigDecimal costAmount = new BigDecimal("100.00");

                assertThatThrownBy(() -> ContractFactory.createFromCommand(null, startDate, endDate, costAmount, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }

            @Test
            @DisplayName("GIVEN now WHEN creating Contract THEN lastModified is the given now")
            void shouldSetLastModifiedToGivenNowOnCreation() {
                LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);

                Contract contract = ContractFactory.createFromCommand(testClientId, now.minusDays(1), null, new BigDecimal("100.00"), now);

                assertThat(contract.getLastModified()).isEqualTo(now);
            }

            @Test
            @DisplayName("GIVEN null now WHEN creating Contract THEN throw InvalidDomainObjectError")
            void shouldRejectNullNowOnCreation() {
                LocalDateTime startDate = LocalDateTime.of(2026, 6, 1, 10, 0);

                assertThatThrownBy(() -> Contract.of(testClientId, ContractPeriod.of(startDate, null, startDate), ContractCost.of(new BigDecimal("100.00")), null))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }
        }

        @Nested
        @DisplayName("From Database")
        class FromDatabase {

            @Test
            @DisplayName("GIVEN persisted lastModified WHEN reconstituting THEN lastModified is kept")
            void shouldKeepPersistedLastModified() {
                LocalDateTime lastModified = LocalDateTime.of(2026, 3, 15, 8, 30);

                Contract contract = ContractFactory.buildFromDatabase(
                        UUID.randomUUID(), testClientId, lastModified.minusMonths(1), null, new BigDecimal("100.00"), lastModified);

                assertThat(contract.getLastModified()).isEqualTo(lastModified);
            }

            @Test
            @DisplayName("GIVEN null lastModified WHEN reconstituting THEN throw InvalidDomainObjectError")
            void shouldRejectNullLastModified() {
                assertThatThrownBy(() -> ContractFactory.buildFromDatabase(
                        UUID.randomUUID(), testClientId, LocalDateTime.of(2026, 1, 1, 0, 0), null, new BigDecimal("100.00"), null))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Nested
        @DisplayName("changeCost")
        class ChangeCost {
            @ParameterizedTest
            @CsvSource({
                    "100.00, 250.75",
                    "50.00, 150.00",
                    "1000.99, 2000.50",
                    "0.01, 99999.99"
            })
            @DisplayName("GIVEN various valid new costs WHEN changing cost THEN cost is updated")
            void shouldUpdateCostWithVariousValues(BigDecimal initialCost, BigDecimal newCost) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);

                Contract contract = ContractFactory.createFromCommand(testClientId, startDate, endDate, initialCost, NOW);
                Contract updatedContract = contract.changeCost(ContractCost.of(newCost), NOW);

                assertThat(updatedContract.getCostAmount().getValue()).isEqualByComparingTo(newCost);
                assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(initialCost);
                assertThat(updatedContract).isNotSameAs(contract);
            }

            @Test
            @DisplayName("GIVEN null cost WHEN changing cost THEN throw InvalidDomainObjectError")
            void shouldRejectNullCostOnUpdate() {
                LocalDateTime startDate = NOW;
                Contract contract = ContractFactory.createFromCommand(testClientId, startDate, startDate.plusDays(30), new BigDecimal("100.00"), NOW);

                assertThatThrownBy(() -> contract.changeCost(null, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class)
                        .hasMessageContaining("Null found for a non-null model attribute");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidCosts")
            @DisplayName("GIVEN invalid cost WHEN changing cost THEN throw InvalidContractCostException")
            void shouldRejectInvalidCostOnUpdate(BigDecimal invalidCost) {
                LocalDateTime startDate = NOW;
                Contract contract = ContractFactory.createFromCommand(testClientId, startDate, startDate.plusDays(30), new BigDecimal("100.00"), NOW);

                assertThatThrownBy(() -> contract.changeCost(ContractCost.of(invalidCost), NOW))
                        .isInstanceOf(InvalidContractCostException.class);
            }


            @Test
            @DisplayName("GIVEN expired contract WHEN changing cost THEN throw ExpiredContractException")
            void shouldRejectCostChangeOnExpiredContract() {
                UUID contractId = UUID.randomUUID();
                LocalDateTime now = NOW;
                LocalDateTime pastStart = now.minusDays(100);
                LocalDateTime pastEnd = now.minusDays(1);
                BigDecimal initialCost = new BigDecimal("100.00");

                Contract contract = ContractFactory.buildFromDatabase(contractId, testClientId, pastStart, pastEnd, initialCost, pastStart);

                assertThatThrownBy(() -> contract.changeCost(ContractCost.of(new BigDecimal("200.00")), NOW))
                        .isInstanceOf(ExpiredContractException.class)
                        .hasMessageContaining(contractId.toString());
            }

            @Test
            @DisplayName("GIVEN expired contract and null cost WHEN changing cost THEN throw InvalidDomainObjectError")
            void shouldRejectNullCostBeforeCheckingExpiration() {
                Contract expiredContract = ContractFactory.buildFromDatabase(
                        UUID.randomUUID(), testClientId, NOW.minusDays(100), NOW.minusDays(1), new BigDecimal("100.00"), NOW.minusDays(100));

                assertThatThrownBy(() -> expiredContract.changeCost(null, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }

            @Test
            @DisplayName("GIVEN new cost WHEN changing cost THEN lastModified is the given now")
            void shouldSetLastModifiedToGivenNowWhenCostChanges() {
                LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
                LocalDateTime changedAt = LocalDateTime.of(2026, 6, 1, 10, 0);
                Contract contract = ContractFactory.createFromCommand(testClientId, createdAt, null, new BigDecimal("100.00"), createdAt);

                Contract updatedContract = contract.changeCost(ContractCost.of(new BigDecimal("200.00")), changedAt);

                assertThat(updatedContract.getLastModified()).isEqualTo(changedAt);
                assertThat(contract.getLastModified()).isEqualTo(createdAt);
            }

            @Test
            @DisplayName("GIVEN same amount with another scale WHEN changing cost THEN contract and lastModified are unchanged")
            void shouldKeepContractUnchangedWhenAmountIsTheSame() {
                LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
                Contract contract = ContractFactory.createFromCommand(testClientId, createdAt, null, new BigDecimal("100.00"), createdAt);

                Contract result = contract.changeCost(ContractCost.of(new BigDecimal("100.0")), LocalDateTime.of(2026, 6, 1, 10, 0));

                assertThat(result).isSameAs(contract);
                assertThat(result.getLastModified()).isEqualTo(createdAt);
            }
        }
    }

    @Nested
    @DisplayName("Contract Status")
    class ContractStatus {

        @Test
        @DisplayName("GIVEN contract with null end date WHEN checking isActive THEN should be active")
        void shouldBeActiveWhenEndDateIsNull() {
            LocalDateTime startDate = NOW.minusDays(10);
            Contract contract = ContractFactory.createFromCommand(testClientId, startDate, null, new BigDecimal("1000.00"), NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
            assertThat(contract.isInactiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("GIVEN contract with future end date WHEN checking isActive THEN should be active")
        void shouldBeActiveWhenEndDateIsInFuture() {
            LocalDateTime startDate = NOW.minusDays(10);
            LocalDateTime endDate = NOW.plusDays(30);
            Contract contract = ContractFactory.createFromCommand(testClientId, startDate, endDate, new BigDecimal("1000.00"), NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
            assertThat(contract.isInactiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("GIVEN contract with past end date WHEN checking isActive THEN should NOT be active")
        void shouldNotBeActiveWhenEndDateIsInPast() {
            LocalDateTime pastStart = NOW.minusDays(100);
            LocalDateTime pastEnd = NOW.minusDays(1);

            Contract contract = ContractFactory.createFromCommand(testClientId, pastStart, pastEnd, new BigDecimal("1000.00"), NOW);

            assertThat(contract.isActiveAt(NOW)).isFalse();
            assertThat(contract.isInactiveAt(NOW)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("com.mk.contractservice.domain.contract.ContractTest#activeContractPeriods")
        @DisplayName("GIVEN various active periods WHEN checking isActive THEN should be active")
        void shouldBeActiveForVariousPeriods(LocalDateTime startDate, LocalDateTime endDate) {
            Contract contract = ContractFactory.createFromCommand(testClientId, startDate, endDate, new BigDecimal("500.00"), NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
        }

        @Test
        @DisplayName("GIVEN fixed reference dates WHEN checking isActiveAt THEN end date is exclusive")
        void shouldEvaluateActivityAgainstGivenReferenceDate() {
            LocalDateTime endDate = LocalDateTime.of(2026, 6, 1, 0, 0);
            Contract contract = ContractFactory.buildFromDatabase(
                    UUID.randomUUID(), testClientId, endDate.minusYears(1), endDate, new BigDecimal("500.00"), endDate.minusYears(1));

            assertThat(contract.isActiveAt(endDate.minusNanos(1))).isTrue();
            assertThat(contract.isActiveAt(endDate)).isFalse();
            assertThat(contract.isActiveAt(endDate.plusDays(1))).isFalse();
        }

        @Test
        @DisplayName("GIVEN no start date WHEN creating contract THEN start date is the given now")
        void shouldDefaultStartDateToGivenNow() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);

            Contract contract = ContractFactory.createFromCommand(testClientId, null, null, new BigDecimal("500.00"), now);

            assertThat(contract.getPeriod().getStartDate()).isEqualTo(now);
        }
    }
}

