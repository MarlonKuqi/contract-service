package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@DisplayName("LamalAffiliation")
class LamalAffiliationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime Y2027 = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime Y2028 = LocalDateTime.of(2028, 1, 1, 0, 0);
    private static final LocalDateTime Y2029 = LocalDateTime.of(2029, 1, 1, 0, 0);
    private static final LocalDateTime Y2030 = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime MID_2027 = Y2027.plusMonths(6);
    private static final LocalDateTime MID_2028 = Y2028.plusMonths(6);
    private static final BigDecimal COST = new BigDecimal("300.00");
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();

    private static Contract existingLamal(final UUID personId, final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.existingContract(UUID.randomUUID(), personId, start, end, COST, NOW);
    }

    private static Contract existingLamal(final LocalDateTime start, final LocalDateTime end) {
        return existingLamal(PERSON_ID, start, end);
    }

    private static Contract newLamal(final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.newContract(PERSON_ID, start, end, COST, NOW);
    }

    private static LamalAffiliation affiliationWith(final Contract... terms) {
        return LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(terms), null);
    }

    @Nested
    @DisplayName("reconstituteFromDatabase")
    class ReconstituteFromDatabase {

        @Test
        @DisplayName("GIVEN no person id WHEN reconstituting an affiliation THEN it is refused")
        void shouldRefuseNullPersonId() {
            assertThatThrownBy(() -> LamalAffiliation.reconstituteFromDatabase(null, List.of(), null))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN no terms WHEN reconstituting an affiliation THEN it is refused")
        void shouldRefuseNullTerms() {
            assertThatThrownBy(() -> LamalAffiliation.reconstituteFromDatabase(PERSON_ID, null, null))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN a stored term without contract id WHEN reconstituting it THEN it is refused")
        void shouldRefuseTermWithoutContractId() {
            ContractPeriod period = ContractPeriod.reconstituteFromDatabase(Y2027, null);

            assertThatThrownBy(() -> LamalTerm.reconstituteFromDatabase(null, period))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN a stored term without period WHEN reconstituting it THEN it is refused")
        void shouldRefuseTermWithoutPeriod() {
            assertThatThrownBy(() -> LamalTerm.reconstituteFromDatabase(UUID.randomUUID(), null))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }
    }

    @Nested
    @DisplayName("ensureAdmits")
    class EnsureAdmits {

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
        @DisplayName("GIVEN an active LAMal WHEN checking a LAMal at any relative position THEN only an overlap is refused")
        void shouldRefuseOnlyOverlappingPositions(final String position,
                final LocalDateTime existingStart,
                final LocalDateTime existingEnd,
                final LocalDateTime newStart,
                final LocalDateTime newEnd,
                final boolean overlaps) {
            LamalAffiliation affiliation = affiliationWith(existingLamal(existingStart, existingEnd));
            Contract candidate = newLamal(newStart, newEnd);

            if (overlaps) {
                assertThatThrownBy(() -> affiliation.ensureAdmits(candidate, NOW))
                        .isInstanceOf(OverlappingContractException.class);
            } else {
                assertThatCode(() -> affiliation.ensureAdmits(candidate, NOW)).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("GIVEN a LAMal in 2027 WHEN checking a LAMal starting during it THEN the refusal names the person, the type and the covered period")
        void shouldDescribeTheOverlappedPeriod() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(Y2027, Y2028));
            Contract overlapping = newLamal(MID_2027, Y2029);

            assertThatThrownBy(() -> affiliation.ensureAdmits(overlapping, NOW))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining(PERSON_ID.toString())
                    .hasMessageContaining("LAMAL")
                    .hasMessageContaining(Y2027.toString());
        }

        @Test
        @DisplayName("GIVEN an open-ended LAMal WHEN checking a later LAMal THEN the refusal says the covered period has no end")
        void shouldDescribeAnOpenEndedPeriod() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(Y2027, null));

            assertThatThrownBy(() -> affiliation.ensureAdmits(newLamal(Y2029, null), NOW))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining("no end date");
        }

        @Test
        @DisplayName("GIVEN a LAMal ending one microsecond after now WHEN checking a LAMal over its period THEN it is refused, the LAMal being still active")
        void shouldRefuseLamalOverlappingOneEndingJustAfterNow() {
            LocalDateTime start = NOW.minusMonths(10);
            LamalAffiliation affiliation = affiliationWith(existingLamal(start, NOW.plus(1, ChronoUnit.MICROS)));

            assertThatThrownBy(() -> affiliation.ensureAdmits(newLamal(start, null), NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a LAMal that already ended WHEN checking a LAMal over its period THEN it is accepted, only active LAMal counting")
        void shouldAcceptLamalOverlappingEndedOne() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(NOW.minusYears(2), NOW.minusYears(1)));
            Contract backdated = newLamal(NOW.minusYears(2).plusMonths(3), NOW.plusYears(1));

            assertThatCode(() -> affiliation.ensureAdmits(backdated, NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a started LAMal closed now by a cancellation WHEN checking the corrected LAMal from the same start THEN it is accepted")
        void shouldAcceptLamalReplacingOneClosedNow() {
            LocalDateTime start = NOW.minusMonths(10);
            LamalAffiliation affiliation = affiliationWith(existingLamal(start, NOW));

            assertThatCode(() -> affiliation.ensureAdmits(newLamal(start, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a still active LAMal that started in the past WHEN checking a LAMal over its past THEN it is refused")
        void shouldRefuseLamalOverlappingThePastOfAnActiveOne() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(NOW.minusYears(1), Y2027));
            Contract overlapping = newLamal(NOW.minusYears(2), NOW.minusMonths(6));

            assertThatThrownBy(() -> affiliation.ensureAdmits(overlapping, NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a future LAMal closed before it started WHEN checking a LAMal over its period THEN it is accepted, the closed one covering nothing")
        void shouldIgnoreLamalClosedBeforeItStarted() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(Y2028, Y2027));

            assertThatCode(() -> affiliation.ensureAdmits(newLamal(Y2027, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a stored LAMal WHEN checking its own new period over the previous one THEN it is accepted, a contract not overlapping itself")
        void shouldNotCompareTheContractWithItself() {
            Contract stored = existingLamal(Y2027, Y2028);
            LamalAffiliation affiliation = affiliationWith(stored);
            Contract extended = stored.changePeriod(ContractPeriod.of(Y2027, Y2029, NOW),
                    ClientFixtures.existingPerson(PERSON_ID).contractHolder(),
                    NOW);

            assertThatCode(() -> affiliation.ensureAdmits(extended, NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN the affiliation of a person WHEN checking a LAMal of another person THEN it is refused")
        void shouldRefuseTermOfAnotherPerson() {
            LamalAffiliation affiliation = LamalAffiliation.reconstituteFromDatabase(PERSON_ID, List.of(), null);
            Contract lamalOfAnotherPerson = existingLamal(OTHER_PERSON_ID, Y2027, null);

            assertThatThrownBy(() -> affiliation.ensureAdmits(lamalOfAnotherPerson, NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN the affiliation of a person WHEN checking a contract other than a LAMal THEN it is refused, only LAMal being its terms")
        void shouldRefuseTermOfAnotherType() {
            LamalAffiliation affiliation = LamalAffiliation.reconstituteFromDatabase(PERSON_ID, List.of(), null);
            Contract laa = ContractFixtures.existingContract(UUID.randomUUID(),
                    PERSON_ID,
                    ContractType.LAA,
                    Y2027,
                    null,
                    COST,
                    NOW);

            assertThatThrownBy(() -> affiliation.ensureAdmits(laa, NOW)).isInstanceOf(InvalidDomainObjectError.class);
        }
    }

    @Nested
    @DisplayName("admit")
    class Admit {

        @Test
        @DisplayName("GIVEN an active LAMal WHEN admitting a LAMal overlapping it THEN it is refused like the check")
        void shouldRefuseOverlappingLamal() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(Y2027, Y2028));

            assertThatThrownBy(() -> affiliation.admit(newLamal(MID_2027, Y2029), NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal WHEN checking a LAMal overlapping it THEN the second one is refused")
        void shouldBlockWithAdmittedLamal() {
            LamalAffiliation admitted = affiliationWith().admit(newLamal(Y2027, null), NOW);

            assertThatThrownBy(() -> admitted.ensureAdmits(newLamal(Y2028, Y2029), NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a refused LAMal WHEN checking a LAMal that only overlaps the refused one THEN it is accepted, the refused one not being kept")
        void shouldNotBlockWithRefusedLamal() {
            LamalAffiliation affiliation = affiliationWith(existingLamal(Y2027, Y2028));
            assertThatThrownBy(() -> affiliation.admit(newLamal(Y2027, Y2029), NOW))
                    .isInstanceOf(OverlappingContractException.class);

            assertThatCode(() -> affiliation.ensureAdmits(newLamal(Y2028, Y2029), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal already ended WHEN checking a LAMal over its period THEN it is accepted, the ended one not blocking")
        void shouldNotBlockWithAdmittedLamalAlreadyEnded() {
            LamalAffiliation admitted = affiliationWith().admit(newLamal(NOW.minusYears(2), NOW.minusYears(1)), NOW);

            assertThatCode(() -> admitted.ensureAdmits(newLamal(NOW.minusYears(2), null), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a stored LAMal moved to a new period WHEN checking a LAMal over its previous period THEN it is accepted, the previous period being replaced")
        void shouldReplaceThePreviousPeriodOfAMovedContract() {
            Contract stored = existingLamal(Y2027, Y2028);
            LamalAffiliation moved = affiliationWith(stored).admit(stored.changePeriod(ContractPeriod.of(Y2029,
                    Y2030,
                    NOW),
                    ClientFixtures.existingPerson(PERSON_ID).contractHolder(),
                    NOW), NOW);

            assertThatCode(() -> moved.ensureAdmits(newLamal(Y2027, Y2028), NOW)).doesNotThrowAnyException();
        }
    }
}
