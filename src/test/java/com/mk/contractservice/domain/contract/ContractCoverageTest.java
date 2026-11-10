package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("ContractCoverage")
class ContractCoverageTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime Y2027 = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime Y2028 = LocalDateTime.of(2028, 1, 1, 0, 0);
    private static final LocalDateTime Y2029 = LocalDateTime.of(2029, 1, 1, 0, 0);
    private static final BigDecimal COST = new BigDecimal("300.00");
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private static Contract existing(final UUID clientId,
            final ContractType type,
            final LocalDateTime start,
            final LocalDateTime end) {
        return ContractFixtures.existingContract(UUID.randomUUID(), clientId, type, start, end, COST, NOW);
    }

    private static Contract newLamal(final UUID clientId, final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.newContract(clientId, start, end, COST, NOW);
    }

    private static ContractCoverage coverageOf(final Contract... existingContracts) {
        return ContractCoverage.of(List.of(existingContracts), NOW);
    }

    private static final LocalDateTime Y2030 = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime MID_2027 = Y2027.plusMonths(6);
    private static final LocalDateTime MID_2028 = Y2028.plusMonths(6);

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
                arguments("open-ended new one starting when the existing one ends", Y2027, Y2028, Y2028, null, false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("relativePositions")
    @DisplayName("GIVEN an active LAMal WHEN adding a LAMal at any relative position THEN only an overlap is refused")
    void shouldRefuseOnlyOverlappingPositions(final String position,
            final LocalDateTime existingStart,
            final LocalDateTime existingEnd,
            final LocalDateTime newStart,
            final LocalDateTime newEnd,
            final boolean overlaps) {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, existingStart, existingEnd));
        Contract candidate = newLamal(PERSON_ID, newStart, newEnd);

        if (overlaps) {
            assertThatThrownBy(() -> coverage.add(candidate)).isInstanceOf(OverlappingContractException.class);
        } else {
            assertThatCode(() -> coverage.add(candidate)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("GIVEN a LAMal ending one microsecond after now WHEN adding a LAMal over its period THEN it is refused, the LAMal being still active")
    void shouldRefuseLamalOverlappingOneEndingJustAfterNow() {
        LocalDateTime start = NOW.minusMonths(10);
        ContractCoverage coverage = coverageOf(existing(PERSON_ID,
                ContractType.LAMAL,
                start,
                NOW.plus(1, ChronoUnit.MICROS)));
        Contract overlapping = newLamal(PERSON_ID, start, null);

        assertThatThrownBy(() -> coverage.add(overlapping)).isInstanceOf(OverlappingContractException.class);
    }

    @Test
    @DisplayName("GIVEN a LAMal in 2027 WHEN adding a LAMal starting when it ends THEN it is accepted, the end date being exclusive")
    void shouldAcceptLamalStartingWhenPreviousOneEnds() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2027, Y2028));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, Y2028, Y2029))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a LAMal in 2027 WHEN adding a LAMal ending when it starts THEN it is accepted")
    void shouldAcceptLamalEndingWhenNextOneStarts() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2028, Y2029));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, Y2027, Y2028))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a LAMal in 2027 WHEN adding a LAMal starting during it THEN it is refused")
    void shouldRefuseLamalStartingDuringExistingOne() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2027, Y2028));
        Contract overlapping = newLamal(PERSON_ID, Y2027.plusMonths(6), Y2029);

        assertThatThrownBy(() -> coverage.add(overlapping))
                .isInstanceOf(OverlappingContractException.class)
                .hasMessageContaining(PERSON_ID.toString())
                .hasMessageContaining("LAMAL")
                .hasMessageContaining(Y2027.toString());
    }

    @Test
    @DisplayName("GIVEN a LAMal in 2028 WHEN adding a LAMal that contains it THEN it is refused")
    void shouldRefuseLamalContainingExistingOne() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2028, Y2029));
        Contract overlapping = newLamal(PERSON_ID, Y2027, null);

        assertThatThrownBy(() -> coverage.add(overlapping)).isInstanceOf(OverlappingContractException.class);
    }

    @Test
    @DisplayName("GIVEN an open-ended LAMal WHEN adding a later LAMal THEN it is refused")
    void shouldRefuseLamalAfterOpenEndedOne() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2027, null));
        Contract overlapping = newLamal(PERSON_ID, Y2029, null);

        assertThatThrownBy(() -> coverage.add(overlapping))
                .isInstanceOf(OverlappingContractException.class)
                .hasMessageContaining("no end date");
    }

    @Test
    @DisplayName("GIVEN a LAMal that already ended WHEN adding a LAMal over its period THEN it is accepted, only active LAMal counting")
    void shouldAcceptLamalOverlappingEndedOne() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID,
                ContractType.LAMAL,
                NOW.minusYears(2),
                NOW.minusYears(1)));
        Contract backdated = newLamal(PERSON_ID, NOW.minusYears(2).plusMonths(3), NOW.plusYears(1));

        assertThatCode(() -> coverage.add(backdated)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a started LAMal closed now by a cancellation WHEN adding the corrected LAMal from the same start THEN it is accepted")
    void shouldAcceptLamalReplacingOneClosedNow() {
        LocalDateTime start = NOW.minusMonths(10);
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, start, NOW));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, start, null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a still active LAMal that started in the past WHEN adding a LAMal over its past THEN it is refused")
    void shouldRefuseLamalOverlappingThePastOfAnActiveOne() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, NOW.minusYears(1), Y2027));
        Contract overlapping = newLamal(PERSON_ID, NOW.minusYears(2), NOW.minusMonths(6));

        assertThatThrownBy(() -> coverage.add(overlapping)).isInstanceOf(OverlappingContractException.class);
    }

    @Test
    @DisplayName("GIVEN a new LAMal already ended WHEN adding a LAMal over its period THEN it is accepted, the ended one not covering")
    void shouldNotCoverWithNewLamalAlreadyEnded() {
        ContractCoverage coverage = coverageOf();
        coverage.add(newLamal(PERSON_ID, NOW.minusYears(2), NOW.minusYears(1)));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, NOW.minusYears(2), null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a future LAMal closed before it started WHEN adding a LAMal over its period THEN it is accepted, the closed one covering nothing")
    void shouldIgnoreLamalClosedBeforeItStarted() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2028, Y2027));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, Y2027, null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a LAMal of another person WHEN adding a LAMal over the same period THEN it is accepted")
    void shouldAcceptLamalOfAnotherPerson() {
        ContractCoverage coverage = coverageOf(existing(UUID.randomUUID(),
                ContractType.LAMAL,
                Y2027,
                null));

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, Y2027, null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a LAA of a company WHEN adding a LAA or a LPP over the same period THEN both are accepted")
    void shouldAcceptOverlappingContractsOfTypesAllowingIt() {
        ContractCoverage coverage = coverageOf(existing(COMPANY_ID, ContractType.LAA, Y2027, null));

        assertThatCode(() -> {
            coverage.add(existing(COMPANY_ID, ContractType.LAA, Y2027, null));
            coverage.add(existing(COMPANY_ID, ContractType.LPP, Y2027, null));
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN an empty coverage WHEN adding two overlapping LAMal THEN the second one is refused")
    void shouldRefuseSecondOverlappingLamalAddedToTheSameCoverage() {
        ContractCoverage coverage = coverageOf();
        coverage.add(newLamal(PERSON_ID, Y2027, null));
        Contract second = newLamal(PERSON_ID, Y2028, Y2029);

        assertThatThrownBy(() -> coverage.add(second)).isInstanceOf(OverlappingContractException.class);
    }

    @Test
    @DisplayName("GIVEN a refused LAMal WHEN adding a LAMal that only overlaps the refused one THEN it is accepted")
    void shouldNotRecordRefusedContract() {
        ContractCoverage coverage = coverageOf(existing(PERSON_ID, ContractType.LAMAL, Y2027, Y2028));
        Contract refused = newLamal(PERSON_ID, Y2027, Y2029);
        assertThatThrownBy(() -> coverage.add(refused)).isInstanceOf(OverlappingContractException.class);

        assertThatCode(() -> coverage.add(newLamal(PERSON_ID, Y2028, Y2029))).doesNotThrowAnyException();
    }
}
