package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractOverlapPolicy")
class ContractOverlapPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime Y2027 = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime Y2028 = LocalDateTime.of(2028, 1, 1, 0, 0);
    private static final LocalDateTime Y2029 = LocalDateTime.of(2029, 1, 1, 0, 0);
    private static final LocalDateTime Y2030 = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime MID_2027 = Y2027.plusMonths(6);
    private static final LocalDateTime MID_2028 = Y2028.plusMonths(6);
    private static final BigDecimal COST = new BigDecimal("300.00");
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Mock
    private ContractRepository contractRepository;

    private ContractOverlapPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ContractOverlapPolicy(contractRepository);
    }

    private static Contract existing(final UUID clientId,
            final ContractType type,
            final LocalDateTime start,
            final LocalDateTime end) {
        return ContractFixtures.existingContract(UUID.randomUUID(), clientId, type, start, end, COST, NOW);
    }

    private static Contract existingLamal(final LocalDateTime start, final LocalDateTime end) {
        return existing(PERSON_ID, ContractType.LAMAL, start, end);
    }

    private static Contract newLamal(final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.newContract(PERSON_ID, start, end, COST, NOW);
    }

    private static Contract newCompanyContract(final ContractType type) {
        return ContractFixtures.newContract(ClientFixtures.existingCompany(COMPANY_ID), type, Y2027, null, COST, NOW);
    }

    private void givenExisting(final Contract... contracts) {
        when(contractRepository.findAllByClientIdsAndTypes(anySet(), anySet())).thenReturn(List.of(contracts));
    }

    @Nested
    @DisplayName("startBatch")
    class StartBatch {

        private ContractOverlapPolicy.Batch overlapBatchOf(final Contract... existingContracts) {
            givenExisting(existingContracts);
            return policy.startBatch(Set.of(PERSON_ID, COMPANY_ID), NOW);
        }

        static Stream<Arguments> relativePositions() {
            return Stream.of(
                    arguments("new one entirely before, with a gap", Y2028, Y2029, Y2027, MID_2027, false),
                    arguments("new one ends when the existing one starts", Y2028, Y2029, Y2027, Y2028, false),
                    arguments("new one ends during the existing one", Y2028, Y2029, Y2027, MID_2028, true),
                    arguments("same start, new one ends first", Y2028, Y2030, Y2028, Y2029, true),
                    arguments("new one strictly inside the existing one", Y2027, Y2030, Y2028, Y2029, true),
                    arguments("same end, new one starts later", Y2027, Y2029, Y2028, Y2029, true),
                    arguments("identical periods", Y2028, Y2029, Y2028, Y2029, true),
                    arguments("new one contains the existing one", Y2028, Y2029, Y2027, Y2030, true),
                    arguments("new one starts during the existing one", Y2027, Y2029, Y2028, Y2030, true),
                    arguments("new one starts when the existing one ends", Y2027, Y2028, Y2028, Y2029, false),
                    arguments("new one entirely after, with a gap", Y2027, Y2028, Y2029, Y2030, false),
                    arguments("open-ended existing one, new one ends when it starts", Y2028, null, Y2027, Y2028, false),
                    arguments("open-ended existing one, new one later", Y2028, null, Y2029, Y2030, true),
                    arguments("both open-ended", Y2028, null, Y2027, null, true),
                    arguments("open-ended new one starting before the existing one", Y2028, Y2029, Y2027, null, true),
                    arguments("open-ended new one starting when the existing one ends",
                            Y2027,
                            Y2028,
                            Y2028,
                            null,
                            false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("relativePositions")
        @DisplayName("GIVEN an active LAMal WHEN admitting a LAMal at any relative position THEN only an overlap is refused")
        void shouldRefuseOnlyOverlappingPositions(final String position,
                final LocalDateTime existingStart,
                final LocalDateTime existingEnd,
                final LocalDateTime newStart,
                final LocalDateTime newEnd,
                final boolean overlaps) {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(existingStart, existingEnd));
            Contract candidate = newLamal(newStart, newEnd);

            if (overlaps) {
                assertThatThrownBy(() -> overlapBatch.admit(candidate)).isInstanceOf(
                        OverlappingContractException.class);
            } else {
                assertThatCode(() -> overlapBatch.admit(candidate)).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("GIVEN a LAMal in 2027 WHEN admitting a LAMal starting during it THEN the refusal names the person, the type and the covered period")
        void shouldDescribeTheOverlappedPeriod() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(Y2027, Y2028));
            Contract overlapping = newLamal(MID_2027, Y2029);

            assertThatThrownBy(() -> overlapBatch.admit(overlapping))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining(PERSON_ID.toString())
                    .hasMessageContaining("LAMAL")
                    .hasMessageContaining(Y2027.toString());
        }

        @Test
        @DisplayName("GIVEN an open-ended LAMal WHEN admitting a later LAMal THEN the refusal says the covered period has no end")
        void shouldDescribeAnOpenEndedPeriod() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(Y2027, null));
            Contract overlapping = newLamal(Y2029, null);

            assertThatThrownBy(() -> overlapBatch.admit(overlapping))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining("no end date");
        }

        @Test
        @DisplayName("GIVEN a LAMal ending one microsecond after now WHEN admitting a LAMal over its period THEN it is refused, the LAMal being still active")
        void shouldRefuseLamalOverlappingOneEndingJustAfterNow() {
            LocalDateTime start = NOW.minusMonths(10);
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(start,
                    NOW.plus(1, ChronoUnit.MICROS)));
            Contract overlapping = newLamal(start, null);

            assertThatThrownBy(() -> overlapBatch.admit(overlapping)).isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a LAMal that already ended WHEN admitting a LAMal over its period THEN it is accepted, only active LAMal counting")
        void shouldAcceptLamalOverlappingEndedOne() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(NOW.minusYears(2),
                    NOW.minusYears(1)));
            Contract backdated = newLamal(NOW.minusYears(2).plusMonths(3), NOW.plusYears(1));

            assertThatCode(() -> overlapBatch.admit(backdated)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a started LAMal closed now by a cancellation WHEN admitting the corrected LAMal from the same start THEN it is accepted")
        void shouldAcceptLamalReplacingOneClosedNow() {
            LocalDateTime start = NOW.minusMonths(10);
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(start, NOW));

            assertThatCode(() -> overlapBatch.admit(newLamal(start, null))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a still active LAMal that started in the past WHEN admitting a LAMal over its past THEN it is refused")
        void shouldRefuseLamalOverlappingThePastOfAnActiveOne() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(NOW.minusYears(1), Y2027));
            Contract overlapping = newLamal(NOW.minusYears(2), NOW.minusMonths(6));

            assertThatThrownBy(() -> overlapBatch.admit(overlapping)).isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a future LAMal closed before it started WHEN admitting a LAMal over its period THEN it is accepted, the closed one covering nothing")
        void shouldIgnoreLamalClosedBeforeItStarted() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(Y2028, Y2027));

            assertThatCode(() -> overlapBatch.admit(newLamal(Y2027, null))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a LAMal of another person WHEN admitting a LAMal over the same period THEN it is accepted")
        void shouldAcceptLamalOfAnotherPerson() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existing(UUID.randomUUID(),
                    ContractType.LAMAL,
                    Y2027,
                    null));

            assertThatCode(() -> overlapBatch.admit(newLamal(Y2027, null))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a company contract of a type allowing overlaps WHEN admitting several contracts of that type over the same period THEN all are accepted")
        void shouldAcceptOverlappingContractsOfTypesAllowingIt(final ContractType type) {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existing(COMPANY_ID, type, Y2027, null));

            assertThatCode(() -> overlapBatch.admit(newCompanyContract(type))).doesNotThrowAnyException();
            assertThatCode(() -> overlapBatch.admit(newCompanyContract(type))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal WHEN admitting a LAMal overlapping it THEN the second one is refused")
        void shouldBlockWithAdmittedLamal() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf();
            Contract accepted = newLamal(Y2027, null);
            Contract overlappingTheAccepted = newLamal(Y2028, Y2029);

            overlapBatch.admit(accepted);

            assertThatThrownBy(() -> overlapBatch.admit(overlappingTheAccepted))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a refused LAMal WHEN admitting a LAMal that only overlaps the refused one THEN it is accepted")
        void shouldNotBlockWithRefusedLamal() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(existingLamal(Y2027, Y2028));
            Contract refused = newLamal(Y2027, Y2029);
            Contract fillingTheGap = newLamal(Y2028, Y2029);
            assertThatThrownBy(() -> overlapBatch.admit(refused)).isInstanceOf(OverlappingContractException.class);

            assertThatCode(() -> overlapBatch.admit(fillingTheGap)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal already ended WHEN admitting a LAMal over its period THEN it is accepted, the ended one not blocking")
        void shouldNotBlockWithAdmittedLamalAlreadyEnded() {
            ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf();
            overlapBatch.admit(newLamal(NOW.minusYears(2), NOW.minusYears(1)));

            assertThatCode(() -> overlapBatch.admit(newLamal(NOW.minusYears(2), null))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ensureNoOverlap")
    class EnsureNoOverlap {

        @Test
        @DisplayName("GIVEN an active LAMal of the person WHEN checking an overlapping LAMal THEN it is refused")
        void shouldRefuseLamalOverlappingExistingOne() {
            givenExisting(existingLamal(Y2027, null));
            Contract overlapping = newLamal(Y2028, Y2029);

            assertThatThrownBy(() -> policy.ensureNoOverlap(overlapping, NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN an active LAMal of the person WHEN checking a LAMal starting when it ends THEN it is accepted")
        void shouldAcceptLamalStartingWhenExistingOneEnds() {
            givenExisting(existingLamal(Y2027, Y2028));

            assertThatCode(() -> policy.ensureNoOverlap(newLamal(Y2028, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a stored LAMal WHEN checking its own new period over the previous one THEN it is accepted, a contract not overlapping itself")
        void shouldNotCompareTheContractWithItself() {
            Contract stored = existingLamal(Y2027, Y2028);
            givenExisting(stored);
            Contract extended = stored.changePeriod(ContractPeriod.of(Y2027, Y2029, NOW), NOW);

            assertThatCode(() -> policy.ensureNoOverlap(extended, NOW)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a type allowing overlaps WHEN checking a contract THEN it is accepted")
        void shouldAcceptTypesAllowingOverlaps(final ContractType type) {
            assertThatCode(() -> policy.ensureNoOverlap(newCompanyContract(type), NOW)).doesNotThrowAnyException();
        }
    }
}
