package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.exception.ContractTypeNotAllowedException;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractFranchiseException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contract - Domain Entity Tests")
class ContractTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);

    private static final ContractHolder ADULT = ContractHolder.person(LocalDate.of(1990, 5, 14));

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
            @CsvSource({"100.00, 30", "1234.56, 365", "0.01, 1", "99999.99, 1000", "50.50, 90", "9999999999.99, 1"
            })
            @DisplayName("GIVEN various valid combinations WHEN creating Contract THEN contract is created with all correct values")
            void shouldCreateContractWithValidCombinations(BigDecimal costAmount, long daysInFuture) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(daysInFuture);

                Contract contract = ContractFixtures.newContract(testClientId,
                        startDate,
                        endDate,
                        costAmount,
                        NOW);

                assertThat(contract.clientId()).isEqualTo(testClientId);
                assertThat(contract.period().startDate()).isEqualTo(startDate);
                assertThat(contract.period().endDate()).isEqualTo(endDate);
                assertThat(contract.costAmount().value()).isEqualByComparingTo(costAmount);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#validPeriods")
            @DisplayName("GIVEN various valid periods WHEN creating Contract THEN contract is created with correct dates")
            void shouldCreateContractWithValidPeriods(LocalDateTime startDate, LocalDateTime endDate) {
                BigDecimal costAmount = new BigDecimal("500.00");

                Contract contract = ContractFixtures.newContract(testClientId,
                        startDate,
                        endDate,
                        costAmount,
                        NOW);

                assertThat(contract.period().startDate()).isNotNull();
                if (startDate == null) {
                    assertThat(contract.period().startDate()).isBeforeOrEqualTo(NOW);
                } else {
                    assertThat(contract.period().startDate()).isEqualTo(startDate);
                }
                assertThat(contract.period().endDate()).isEqualTo(endDate);
                assertThat(contract.clientId()).isEqualTo(testClientId);
                assertThat(contract.costAmount().value()).isEqualByComparingTo(costAmount);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidCosts")
            @DisplayName("GIVEN invalid cost WHEN creating Contract THEN throw InvalidContractCostException")
            void shouldRejectInvalidCost(BigDecimal invalidCost) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);

                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId,
                        startDate,
                        endDate,
                        invalidCost,
                        NOW))
                        .isInstanceOf(InvalidContractCostException.class);
            }

            @ParameterizedTest
            @CsvSource({"-0.001, decimal places", "-99999999999999, integer digits"
            })
            @DisplayName("GIVEN negative cost with an invalid precision WHEN creating Contract THEN the precision error is reported first")
            void shouldReportPrecisionBeforePositivity(BigDecimal invalidCost, String expectedMessage) {
                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId,
                        NOW,
                        NOW.plusDays(30),
                        invalidCost,
                        NOW))
                        .isInstanceOf(InvalidContractCostException.class)
                        .hasMessageContaining(expectedMessage);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidPeriods")
            @DisplayName("GIVEN invalid period WHEN creating Contract THEN throw InvalidContractPeriodException")
            void shouldRejectInvalidPeriod(LocalDateTime startDate, LocalDateTime endDate) {
                BigDecimal costAmount = new BigDecimal("100.00");

                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId,
                        startDate,
                        endDate,
                        costAmount,
                        NOW))
                        .isInstanceOf(InvalidContractPeriodException.class);
            }


            @Test
            @DisplayName("GIVEN a year PostgreSQL may not store WHEN creating Contract THEN throw InvalidContractPeriodException")
            void shouldRejectYearOutOfRange() {
                BigDecimal costAmount = new BigDecimal("100.00");
                LocalDateTime tooLate = LocalDateTime.of(10000, 1, 1, 0, 0);
                LocalDateTime tooEarly = LocalDateTime.of(0, 12, 31, 0, 0);

                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId,
                        tooLate,
                        null,
                        costAmount,
                        NOW))
                        .isInstanceOf(InvalidContractPeriodException.class);
                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId,
                        tooEarly,
                        NOW,
                        costAmount,
                        NOW))
                        .isInstanceOf(InvalidContractPeriodException.class);
                assertThatThrownBy(() -> ContractFixtures.newContract(testClientId, NOW, tooLate, costAmount, NOW))
                        .isInstanceOf(InvalidContractPeriodException.class);
            }

            @Test
            @DisplayName("GIVEN the first and last accepted years WHEN creating Contract THEN it is created")
            void shouldAcceptBoundaryYears() {
                LocalDateTime first = LocalDateTime.of(1, 1, 1, 0, 0);
                LocalDateTime last = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

                Contract contract = ContractFixtures.newContract(ClientFixtures.existingCompany(testClientId),
                        ContractType.LAA,
                        first,
                        last,
                        new BigDecimal("100.00"),
                        NOW);

                assertThat(contract.period().startDate()).isEqualTo(first);
                assertThat(contract.period().endDate()).isEqualTo(last);
            }

            @Test
            @DisplayName("GIVEN dates finer than the microsecond WHEN creating Contract THEN they are truncated to the stored precision")
            void shouldTruncateDatesToMicroseconds() {
                LocalDateTime start = LocalDateTime.of(2027, 1, 1, 0, 0, 0, 123_456_789);
                LocalDateTime end = LocalDateTime.of(2028, 1, 1, 0, 0, 0, 987_654_321);

                Contract contract = ContractFixtures.newContract(testClientId,
                        start,
                        end,
                        new BigDecimal("100.00"),
                        NOW);

                assertThat(contract.period().startDate()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0, 0, 123_456_000));
                assertThat(contract.period().endDate()).isEqualTo(LocalDateTime.of(2028, 1, 1, 0, 0, 0, 987_654_000));
            }

            @Test
            @DisplayName("GIVEN null client ID WHEN creating Contract THEN throw InvalidDomainObjectError")
            void shouldRejectNullClientId() {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);
                BigDecimal costAmount = new BigDecimal("100.00");

                assertThatThrownBy(() -> ContractFixtures.newContract(null, startDate, endDate, costAmount, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }

            @Test
            @DisplayName("GIVEN now WHEN creating Contract THEN lastModified is the given now")
            void shouldSetLastModifiedToGivenNowOnCreation() {
                LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);

                Contract contract = ContractFixtures.newContract(testClientId,
                        now.minusDays(1),
                        null,
                        new BigDecimal("100.00"),
                        now);

                assertThat(contract.lastModified()).isEqualTo(now);
            }

            @Test
            @DisplayName("GIVEN null now WHEN creating Contract THEN throw InvalidDomainObjectError")
            void shouldRejectNullNowOnCreation() {
                LocalDateTime startDate = LocalDateTime.of(2026, 6, 1, 10, 0);

                assertThatThrownBy(() -> Contract.of(testClientId,
                        ADULT,
                        ContractCoverage.of(ContractType.LAMAL, ContractFixtures.ADULT_FRANCHISE),
                        ContractPeriod.of(startDate, null, startDate),
                        ContractCost.of(new BigDecimal("100.00")),
                        null))
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

                Contract contract = ContractFixtures.existingContract(
                        UUID.randomUUID(),
                        testClientId,
                        lastModified.minusMonths(1),
                        null,
                        new BigDecimal("100.00"),
                        lastModified);

                assertThat(contract.lastModified()).isEqualTo(lastModified);
            }

            @Test
            @DisplayName("GIVEN null lastModified WHEN reconstituting THEN throw InvalidDomainObjectError")
            void shouldRejectNullLastModified() {
                assertThatThrownBy(() -> ContractFixtures.existingContract(
                        UUID.randomUUID(),
                        testClientId,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        null,
                        new BigDecimal("100.00"),
                        null))
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
            @CsvSource({"100.00, 250.75", "50.00, 150.00", "1000.99, 2000.50", "0.01, 99999.99"
            })
            @DisplayName("GIVEN various valid new costs WHEN changing cost THEN cost is updated")
            void shouldUpdateCostWithVariousValues(BigDecimal initialCost, BigDecimal newCost) {
                LocalDateTime startDate = NOW;
                LocalDateTime endDate = startDate.plusDays(30);

                Contract contract = ContractFixtures.newContract(testClientId,
                        startDate,
                        endDate,
                        initialCost,
                        NOW);
                Contract updatedContract = contract.changeCost(ContractCost.of(newCost), NOW);

                assertThat(updatedContract.costAmount().value()).isEqualByComparingTo(newCost);
                assertThat(contract.costAmount().value()).isEqualByComparingTo(initialCost);
                assertThat(updatedContract).isNotSameAs(contract);
            }

            @Test
            @DisplayName("GIVEN null cost WHEN changing cost THEN throw InvalidDomainObjectError")
            void shouldRejectNullCostOnUpdate() {
                LocalDateTime startDate = NOW;
                Contract contract = ContractFixtures.newContract(testClientId,
                        startDate,
                        startDate.plusDays(30),
                        new BigDecimal("100.00"),
                        NOW);

                assertThatThrownBy(() -> contract.changeCost(null, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class)
                        .hasMessageContaining("Null found for a non-null model attribute");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.contract.ContractTest#invalidCosts")
            @DisplayName("GIVEN invalid cost WHEN changing cost THEN throw InvalidContractCostException")
            void shouldRejectInvalidCostOnUpdate(BigDecimal invalidCost) {
                LocalDateTime startDate = NOW;
                Contract contract = ContractFixtures.newContract(testClientId,
                        startDate,
                        startDate.plusDays(30),
                        new BigDecimal("100.00"),
                        NOW);

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

                Contract contract = ContractFixtures.existingContract(contractId,
                        testClientId,
                        pastStart,
                        pastEnd,
                        initialCost,
                        pastStart);

                assertThatThrownBy(() -> contract.changeCost(ContractCost.of(new BigDecimal("200.00")), NOW))
                        .isInstanceOf(ExpiredContractException.class)
                        .hasMessageContaining(contractId.toString());
            }

            @Test
            @DisplayName("GIVEN expired contract and null cost WHEN changing cost THEN throw InvalidDomainObjectError")
            void shouldRejectNullCostBeforeCheckingExpiration() {
                Contract expiredContract = ContractFixtures.existingContract(
                        UUID.randomUUID(),
                        testClientId,
                        NOW.minusDays(100),
                        NOW.minusDays(1),
                        new BigDecimal("100.00"),
                        NOW.minusDays(100));

                assertThatThrownBy(() -> expiredContract.changeCost(null, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }

            @Test
            @DisplayName("GIVEN new cost WHEN changing cost THEN lastModified is the given now")
            void shouldSetLastModifiedToGivenNowWhenCostChanges() {
                LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
                LocalDateTime changedAt = LocalDateTime.of(2026, 6, 1, 10, 0);
                Contract contract = ContractFixtures.newContract(testClientId,
                        createdAt,
                        null,
                        new BigDecimal("100.00"),
                        createdAt);

                Contract updatedContract = contract.changeCost(ContractCost.of(new BigDecimal("200.00")), changedAt);

                assertThat(updatedContract.lastModified()).isEqualTo(changedAt);
                assertThat(contract.lastModified()).isEqualTo(createdAt);
            }

            @Test
            @DisplayName("GIVEN same amount with another scale WHEN changing cost THEN contract and lastModified are unchanged")
            void shouldKeepContractUnchangedWhenAmountIsTheSame() {
                LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
                Contract contract = ContractFixtures.newContract(testClientId,
                        createdAt,
                        null,
                        new BigDecimal("100.00"),
                        createdAt);

                Contract result = contract.changeCost(ContractCost.of(new BigDecimal("100.0")),
                        LocalDateTime.of(2026, 6, 1, 10, 0));

                assertThat(result).isSameAs(contract);
                assertThat(result.lastModified()).isEqualTo(createdAt);
            }
        }

        @Nested
        @DisplayName("Change Period")
        class ChangePeriod {

            private final LocalDateTime createdAt = NOW.minusMonths(3);

            private Contract activeContract(final LocalDateTime startDate, final LocalDateTime endDate) {
                return ContractFixtures.existingContract(UUID.randomUUID(),
                        testClientId,
                        startDate,
                        endDate,
                        new BigDecimal("100.00"),
                        createdAt);
            }

            @Test
            @DisplayName("GIVEN active contract WHEN changing period THEN period is replaced and lastModified is the given now")
            void shouldReplacePeriodAndSetLastModified() {
                Contract contract = activeContract(NOW.minusMonths(3), NOW.plusMonths(9));
                ContractPeriod newPeriod = ContractPeriod.of(NOW.minusMonths(2), NOW.plusYears(1), NOW);

                Contract updated = contract.changePeriod(newPeriod, ADULT, NOW);

                assertThat(updated.period()).isEqualTo(newPeriod);
                assertThat(updated.lastModified()).isEqualTo(NOW);
                assertThat(updated.id()).isEqualTo(contract.id());
                assertThat(updated.type()).isEqualTo(contract.type());
                assertThat(updated.costAmount()).isEqualTo(contract.costAmount());
                assertThat(contract.lastModified()).isEqualTo(createdAt);
            }

            @Test
            @DisplayName("GIVEN active contract WHEN moving start date into the past THEN period is changed")
            void shouldAcceptRetroactiveStartDate() {
                Contract contract = activeContract(NOW.plusDays(10), null);
                ContractPeriod newPeriod = ContractPeriod.of(NOW.minusYears(1), null, NOW);

                Contract updated = contract.changePeriod(newPeriod, ADULT, NOW);

                assertThat(updated.period().startDate()).isEqualTo(NOW.minusYears(1));
            }

            @Test
            @DisplayName("GIVEN contract with end date WHEN removing end date THEN contract is open-ended")
            void shouldRemoveEndDate() {
                Contract contract = activeContract(NOW.minusMonths(3), NOW.plusMonths(9));

                Contract updated = contract.changePeriod(ContractPeriod.of(NOW.minusMonths(3), null, NOW), ADULT, NOW);

                assertThat(updated.period().endDate()).isNull();
            }

            @Test
            @DisplayName("GIVEN end date equal to now WHEN changing period THEN contract is closed now")
            void shouldCloseContractNow() {
                Contract contract = activeContract(NOW.minusMonths(3), null);

                Contract updated = contract.changePeriod(ContractPeriod.of(NOW.minusMonths(3), NOW, NOW), ADULT, NOW);

                assertThat(updated.period().endDate()).isEqualTo(NOW);
                assertThat(updated.isActiveAt(NOW)).isFalse();
            }

            @Test
            @DisplayName("GIVEN end date in the past WHEN changing period THEN throw InvalidContractPeriodException")
            void shouldRejectRetroactiveEndDate() {
                Contract contract = activeContract(NOW.minusMonths(3), null);
                ContractPeriod retroactiveEnd = ContractPeriod.of(NOW.minusMonths(3), NOW.minusSeconds(1), NOW);

                assertThatThrownBy(() -> contract.changePeriod(retroactiveEnd, ADULT, NOW))
                        .isInstanceOf(InvalidContractPeriodException.class)
                        .hasMessageContaining("past");
            }

            @Test
            @DisplayName("GIVEN expired contract WHEN changing period THEN throw ExpiredContractException")
            void shouldRejectPeriodChangeOnExpiredContract() {
                Contract expired = activeContract(NOW.minusMonths(3), NOW.minusDays(1));

                assertThatThrownBy(() -> expired.changePeriod(ContractPeriod.of(NOW.minusMonths(3), null, NOW),
                        ADULT,
                        NOW))
                        .isInstanceOf(ExpiredContractException.class)
                        .hasMessageContaining(expired.id().toString());
            }

            @Test
            @DisplayName("GIVEN expired contract and null period WHEN changing period THEN throw InvalidDomainObjectError")
            void shouldRejectNullPeriodBeforeCheckingExpiration() {
                Contract expired = activeContract(NOW.minusMonths(3), NOW.minusDays(1));

                assertThatThrownBy(() -> expired.changePeriod(null, ADULT, NOW))
                        .isInstanceOf(InvalidDomainObjectError.class);
            }

            @Test
            @DisplayName("GIVEN same period WHEN changing period THEN contract and lastModified are unchanged")
            void shouldKeepContractUnchangedWhenPeriodIsTheSame() {
                Contract contract = activeContract(NOW.minusMonths(3), NOW.plusMonths(9));

                Contract result = contract.changePeriod(ContractPeriod.of(NOW.minusMonths(3), NOW.plusMonths(9), NOW),
                        ADULT,
                        NOW);

                assertThat(result).isSameAs(contract);
                assertThat(result.lastModified()).isEqualTo(createdAt);
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
            Contract contract = ContractFixtures.newContract(testClientId,
                    startDate,
                    null,
                    new BigDecimal("1000.00"),
                    NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
            assertThat(contract.isInactiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("GIVEN contract with future end date WHEN checking isActive THEN should be active")
        void shouldBeActiveWhenEndDateIsInFuture() {
            LocalDateTime startDate = NOW.minusDays(10);
            LocalDateTime endDate = NOW.plusDays(30);
            Contract contract = ContractFixtures.newContract(testClientId,
                    startDate,
                    endDate,
                    new BigDecimal("1000.00"),
                    NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
            assertThat(contract.isInactiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("GIVEN contract with past end date WHEN checking isActive THEN should NOT be active")
        void shouldNotBeActiveWhenEndDateIsInPast() {
            LocalDateTime pastStart = NOW.minusDays(100);
            LocalDateTime pastEnd = NOW.minusDays(1);

            Contract contract = ContractFixtures.newContract(testClientId,
                    pastStart,
                    pastEnd,
                    new BigDecimal("1000.00"),
                    NOW);

            assertThat(contract.isActiveAt(NOW)).isFalse();
            assertThat(contract.isInactiveAt(NOW)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("com.mk.contractservice.domain.contract.ContractTest#activeContractPeriods")
        @DisplayName("GIVEN various active periods WHEN checking isActive THEN should be active")
        void shouldBeActiveForVariousPeriods(LocalDateTime startDate, LocalDateTime endDate) {
            Contract contract = ContractFixtures.newContract(testClientId,
                    startDate,
                    endDate,
                    new BigDecimal("500.00"),
                    NOW);

            assertThat(contract.isActiveAt(NOW)).isTrue();
        }

        @Test
        @DisplayName("GIVEN fixed reference dates WHEN checking isActiveAt THEN end date is exclusive")
        void shouldEvaluateActivityAgainstGivenReferenceDate() {
            LocalDateTime endDate = LocalDateTime.of(2026, 6, 1, 0, 0);
            Contract contract = ContractFixtures.existingContract(
                    UUID.randomUUID(),
                    testClientId,
                    endDate.minusYears(1),
                    endDate,
                    new BigDecimal("500.00"),
                    endDate.minusYears(1));

            assertThat(contract.isActiveAt(endDate.minusNanos(1))).isTrue();
            assertThat(contract.isActiveAt(endDate)).isFalse();
            assertThat(contract.isActiveAt(endDate.plusDays(1))).isFalse();
        }

        @Test
        @DisplayName("GIVEN no start date WHEN creating contract THEN start date is the given now")
        void shouldDefaultStartDateToGivenNow() {
            LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);

            Contract contract = ContractFixtures.newContract(testClientId,
                    null,
                    null,
                    new BigDecimal("500.00"),
                    now);

            assertThat(contract.period().startDate()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Contract Type")
    class Type {

        private static final BigDecimal COST = new BigDecimal("350.00");

        @Test
        @DisplayName("GIVEN a person WHEN taking out a LAMal contract THEN the contract is created with its type")
        void shouldAllowLamalForPerson() {
            Client person = ClientFixtures.existingPerson(testClientId);

            Contract contract = ContractFixtures.newContract(person, ContractType.LAMAL, NOW, null, COST, NOW);

            assertThat(contract.type()).isEqualTo(ContractType.LAMAL);
            assertThat(contract.clientId()).isEqualTo(testClientId);
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a company WHEN taking out a LAA or LPP contract THEN the contract is created with its type")
        void shouldAllowLaaAndLppForCompany(ContractType type) {
            Client company = ClientFixtures.existingCompany(testClientId);

            Contract contract = ContractFixtures.newContract(company, type, NOW, null, COST, NOW);

            assertThat(contract.type()).isEqualTo(type);
            assertThat(contract.clientId()).isEqualTo(testClientId);
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a person WHEN taking out a LAA or LPP contract THEN it is refused: these insurances are taken out by employers")
        void shouldRefuseLaaAndLppForPerson(ContractType type) {
            Client person = ClientFixtures.existingPerson(testClientId);

            assertThatThrownBy(() -> ContractFixtures.newContract(person, type, NOW, null, COST, NOW))
                    .isInstanceOf(ContractTypeNotAllowedException.class)
                    .hasMessageContaining(type.name())
                    .hasMessageContaining("person");
        }

        @Test
        @DisplayName("GIVEN a company WHEN taking out a LAMal contract THEN it is refused: health insurance covers a person")
        void shouldRefuseLamalForCompany() {
            Client company = ClientFixtures.existingCompany(testClientId);

            assertThatThrownBy(() -> ContractFixtures.newContract(company, ContractType.LAMAL, NOW, null, COST, NOW))
                    .isInstanceOf(ContractTypeNotAllowedException.class)
                    .hasMessageContaining("LAMAL")
                    .hasMessageContaining("company");
        }

        @Test
        @DisplayName("GIVEN no type WHEN creating Contract THEN throw InvalidDomainObjectError")
        void shouldRejectNullType() {
            Client person = ClientFixtures.existingPerson(testClientId);

            assertThatThrownBy(() -> ContractFixtures.newContract(person, null, NOW, null, COST, NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN no contract holder WHEN creating Contract THEN throw InvalidDomainObjectError")
        void shouldRejectNullContractHolder() {
            assertThatThrownBy(() -> Contract.of(testClientId,
                    null,
                    ContractCoverage.of(ContractType.LAMAL, ContractFixtures.ADULT_FRANCHISE),
                    ContractPeriod.of(NOW, null, NOW),
                    ContractCost.of(COST),
                    NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN a contract WHEN changing its cost THEN its type is unchanged")
        void shouldKeepTypeWhenChangingCost() {
            Contract contract = ContractFixtures.newContract(ClientFixtures.existingCompany(testClientId),
                    ContractType.LPP,
                    NOW,
                    null,
                    COST,
                    NOW);

            Contract updated = contract.changeCost(ContractCost.of(new BigDecimal("400.00")), NOW.plusDays(1));

            assertThat(updated.type()).isEqualTo(ContractType.LPP);
        }

        @Test
        @DisplayName("GIVEN a persisted type WHEN reconstituting THEN the type is kept")
        void shouldKeepPersistedType() {
            Contract contract = ContractFixtures.existingContract(UUID.randomUUID(),
                    testClientId,
                    ContractType.LAA,
                    NOW.minusMonths(1),
                    null,
                    COST,
                    NOW);

            assertThat(contract.type()).isEqualTo(ContractType.LAA);
        }

        @Test
        @DisplayName("GIVEN no type WHEN reconstituting THEN throw InvalidDomainObjectError")
        void shouldRejectNullTypeFromDatabase() {
            assertThatThrownBy(() -> ContractFixtures.existingContract(UUID.randomUUID(),
                    testClientId,
                    null,
                    NOW.minusMonths(1),
                    null,
                    COST,
                    NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }
    }


    @Nested
    @DisplayName("LAMal Franchise")
    class Franchise {

        private static final BigDecimal COST = new BigDecimal("350.00");
        private static final LocalDate CHILD_BIRTH_DATE = LocalDate.of(2012, 3, 10);
        private static final LocalDateTime END_OF_CHILDHOOD = LocalDateTime.of(2031, 1, 1, 0, 0);
        private static final LocalDate BORN_ON_NEW_YEARS_EVE_2008 = LocalDate.of(2008, 12, 31);

        private Contract lamal(final LocalDate birthDate,
                final Integer franchise,
                final LocalDateTime start,
                final LocalDateTime end) {
            return ContractFixtures.newContract(ClientFixtures.existingPerson(testClientId, birthDate),
                    ContractType.LAMAL,
                    franchise,
                    start,
                    end,
                    COST,
                    NOW);
        }

        @ParameterizedTest
        @ValueSource(ints = {300, 500, 1000, 1500, 2000, 2500})
        @DisplayName("GIVEN an adult WHEN taking out a LAMal with an adult franchise THEN the contract carries it")
        void shouldAcceptAdultFranchises(final int franchise) {
            Contract contract = lamal(LocalDate.of(1990, 5, 14), franchise, NOW, null);

            assertThat(contract.coverage()).isEqualTo(ContractCoverage.of(ContractType.LAMAL, franchise));
            assertThat(contract.type()).isEqualTo(ContractType.LAMAL);
            assertThat(contract.franchise().map(LamalFranchise::value)).hasValue(franchise);
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a company WHEN taking out a LAA or LPP THEN the contract carries no franchise")
        void shouldCarryNoFranchiseForOtherTypes(final ContractType type) {
            Contract contract = ContractFixtures.newContract(ClientFixtures.existingCompany(testClientId),
                    type,
                    null,
                    NOW,
                    null,
                    COST,
                    NOW);

            assertThat(contract.franchise()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100, 200, 400, 600})
        @DisplayName("GIVEN an adult WHEN taking out a LAMal with a child-only franchise THEN it is refused")
        void shouldRefuseChildOnlyFranchisesForAdult(final int franchise) {
            assertThatThrownBy(() -> lamal(LocalDate.of(1990, 5, 14), franchise, NOW, null))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("adult")
                    .hasMessageContaining(String.valueOf(franchise));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100, 200, 300, 400, 500, 600})
        @DisplayName("GIVEN a child WHEN taking out a LAMal ending with childhood and a child franchise THEN the contract carries it")
        void shouldAcceptChildFranchises(final int franchise) {
            Contract contract = lamal(CHILD_BIRTH_DATE, franchise, NOW, END_OF_CHILDHOOD);

            assertThat(contract.coverage()).isEqualTo(ContractCoverage.of(ContractType.LAMAL, franchise));
        }

        @ParameterizedTest
        @ValueSource(ints = {1000, 1500, 2000, 2500})
        @DisplayName("GIVEN a child WHEN taking out a LAMal with an adult-only franchise THEN it is refused")
        void shouldRefuseAdultOnlyFranchisesForChild(final int franchise) {
            assertThatThrownBy(() -> lamal(CHILD_BIRTH_DATE, franchise, NOW, END_OF_CHILDHOOD))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("child")
                    .hasMessageContaining(String.valueOf(franchise));
        }

        @ParameterizedTest
        @ValueSource(ints = {-100, 50, 250, 700, 3000})
        @DisplayName("GIVEN an amount outside both legal scales WHEN building a LAMal coverage THEN it is refused whatever the age of the insured")
        void shouldRefuseAmountsOutsideLegalFranchises(final int franchise) {
            assertThatThrownBy(() -> ContractCoverage.of(ContractType.LAMAL, franchise))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining(String.valueOf(franchise));
        }

        @Test
        @DisplayName("GIVEN a child WHEN taking out an open-ended LAMal THEN it is refused: it must end on January 1st of the year they turn 19")
        void shouldRefuseOpenEndedChildLamal() {
            assertThatThrownBy(() -> lamal(CHILD_BIRTH_DATE, 0, NOW, null))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("2031-01-01");
        }

        @Test
        @DisplayName("GIVEN a child WHEN taking out a LAMal ending after childhood THEN it is refused")
        void shouldRefuseChildLamalEndingAfterChildhood() {
            assertThatThrownBy(() -> lamal(CHILD_BIRTH_DATE, 0, NOW, END_OF_CHILDHOOD.plusSeconds(1)))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("2031-01-01");
        }

        @Test
        @DisplayName("GIVEN a person born on December 31st WHEN the LAMal starts in the year they turn 18 THEN they are a child")
        void shouldUseBirthYearForChildCategory() {
            LocalDateTime lastMomentOfChildhood = LocalDateTime.of(2026, 12, 31, 23, 0);

            Contract contract = lamal(BORN_ON_NEW_YEARS_EVE_2008,
                    0,
                    lastMomentOfChildhood,
                    LocalDateTime.of(2027, 1, 1, 0, 0));

            assertThat(contract.coverage()).isEqualTo(ContractCoverage.of(ContractType.LAMAL, 0));
        }

        @Test
        @DisplayName("GIVEN a person born on December 31st WHEN the LAMal starts on January 1st of the year they turn 19 THEN they are an adult")
        void shouldUseBirthYearForAdultCategory() {
            LocalDateTime firstMomentOfAdulthood = LocalDateTime.of(2027, 1, 1, 0, 0);

            assertThatThrownBy(() -> lamal(BORN_ON_NEW_YEARS_EVE_2008, 0, firstMomentOfAdulthood, null))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("adult");
            assertThat(lamal(BORN_ON_NEW_YEARS_EVE_2008, 300, firstMomentOfAdulthood, null).coverage())
                    .isEqualTo(ContractCoverage.of(ContractType.LAMAL, 300));
        }

        @Test
        @DisplayName("GIVEN no franchise WHEN taking out a LAMal THEN it is refused")
        void shouldRequireFranchiseForLamal() {
            assertThatThrownBy(() -> lamal(LocalDate.of(1990, 5, 14), null, NOW, null))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("LAMAL");
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a franchise WHEN taking out a LAA or LPP THEN it is refused: only the LAMal has a franchise")
        void shouldRefuseFranchiseForOtherTypes(final ContractType type) {
            assertThatThrownBy(() -> ContractFixtures.newContract(ClientFixtures.existingCompany(testClientId),
                    type,
                    300,
                    NOW,
                    null,
                    COST,
                    NOW))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining(type.name());
        }

        @Test
        @DisplayName("GIVEN an adult LAMal WHEN moving its start into childhood THEN it is refused with an adult-only franchise")
        void shouldRecheckFranchiseWhenStartMovesIntoChildhood() {
            ContractHolder holder = ContractHolder.person(BORN_ON_NEW_YEARS_EVE_2008);
            Contract contract = lamal(BORN_ON_NEW_YEARS_EVE_2008, 1000, LocalDateTime.of(2027, 1, 1, 0, 0), null);
            ContractPeriod intoChildhood = ContractPeriod.of(LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2027, 1, 1, 0, 0),
                    NOW);

            assertThatThrownBy(() -> contract.changePeriod(intoChildhood, holder, NOW))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("child");
        }

        @Test
        @DisplayName("GIVEN a child LAMal WHEN removing its end date THEN it is refused")
        void shouldRefuseOpenEndedChildLamalOnPeriodChange() {
            ContractHolder holder = ContractHolder.person(CHILD_BIRTH_DATE);
            Contract contract = lamal(CHILD_BIRTH_DATE, 0, NOW, END_OF_CHILDHOOD);

            assertThatThrownBy(() -> contract.changePeriod(ContractPeriod.of(NOW, null, NOW), holder, NOW))
                    .isInstanceOf(InvalidContractFranchiseException.class)
                    .hasMessageContaining("2031-01-01");
        }

        @Test
        @DisplayName("GIVEN a child LAMal WHEN shortening it THEN the period is changed")
        void shouldAcceptChildLamalPeriodWithinChildhood() {
            ContractHolder holder = ContractHolder.person(CHILD_BIRTH_DATE);
            Contract contract = lamal(CHILD_BIRTH_DATE, 0, NOW, END_OF_CHILDHOOD);
            ContractPeriod shorter = ContractPeriod.of(NOW, LocalDateTime.of(2028, 1, 1, 0, 0), NOW);

            assertThat(contract.changePeriod(shorter, holder, NOW).period()).isEqualTo(shorter);
        }

        @Test
        @DisplayName("GIVEN a LAMal WHEN changing its cost THEN its franchise is unchanged")
        void shouldKeepFranchiseWhenChangingCost() {
            Contract contract = lamal(LocalDate.of(1990, 5, 14), 2500, NOW, null);

            Contract updated = contract.changeCost(ContractCost.of(new BigDecimal("400.00")), NOW.plusDays(1));

            assertThat(updated.coverage()).isEqualTo(ContractCoverage.of(ContractType.LAMAL, 2500));
        }

        @Test
        @DisplayName("GIVEN a persisted franchise WHEN reconstituting THEN the franchise is kept")
        void shouldKeepPersistedFranchise() {
            Contract contract = ContractFixtures.existingContract(UUID.randomUUID(),
                    testClientId,
                    ContractType.LAMAL,
                    1500,
                    NOW.minusMonths(1),
                    null,
                    COST,
                    NOW);

            assertThat(contract.coverage()).isEqualTo(ContractCoverage.of(ContractType.LAMAL, 1500));
        }

        @Test
        @DisplayName("GIVEN a persisted LAMal without franchise WHEN reconstituting THEN throw InvalidDomainObjectError")
        void shouldRejectPersistedLamalWithoutFranchise() {
            assertThatThrownBy(() -> ContractFixtures.existingContract(UUID.randomUUID(),
                    testClientId,
                    ContractType.LAMAL,
                    null,
                    NOW.minusMonths(1),
                    null,
                    COST,
                    NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }
    }
}
