package com.mk.contractservice.features.lamal;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.lamal.LamalAffiliation;
import com.mk.contractservice.domain.lamal.LamalAffiliationFixtures;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.domain.lamal.exception.InvalidLamalExemptionPeriodException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionConflictException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotAllowedException;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.domain.shared.Residence;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordLamalExemption")
class RecordLamalExemptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 14);
    private static final LocalDate JAN_2026 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_2027 = LocalDate.of(2027, 1, 1);
    private static final UUID PERSON_ID = UUID.randomUUID();

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LamalAffiliationRepository lamalAffiliationRepository;

    private RecordLamalExemption.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new RecordLamalExemption.Handler(clientRepository, lamalAffiliationRepository, FIXED_CLOCK);
    }

    private static LamalExemption exemption(final LocalDate start, final LocalDate end) {
        return LamalExemption.of(start, end, ContractHolder.person(BIRTH_DATE, Residence.FR));
    }

    @Test
    @DisplayName("GIVEN a cross-border worker without exemption WHEN recording one THEN the affiliation is saved with it")
    void shouldRecordExemption() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.FR));
        LamalAffiliation affiliation = LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(), null);
        when(lamalAffiliationRepository.loadForUpdate(PERSON_ID)).thenReturn(affiliation);

        LamalExemption recorded = handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, null));

        assertThat(recorded).isEqualTo(exemption(JAN_2026, null));
        verify(lamalAffiliationRepository).save(
                LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(), exemption(JAN_2026, null)));
    }

    @Test
    @DisplayName("GIVEN an ongoing exemption WHEN closing it THEN the affiliation is saved with the end date")
    void shouldCloseExemption() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.FR));
        when(lamalAffiliationRepository.loadForUpdate(PERSON_ID)).thenReturn(
                LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(), exemption(JAN_2026, null)));

        handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, JAN_2027));

        verify(lamalAffiliationRepository).save(
                LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(), exemption(JAN_2026, JAN_2027)));
    }

    @Test
    @DisplayName("GIVEN the same exemption already recorded WHEN recording it again THEN nothing is saved")
    void shouldNotSaveSameExemption() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.FR));
        when(lamalAffiliationRepository.loadForUpdate(PERSON_ID)).thenReturn(
                LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(), exemption(JAN_2026, null)));

        handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, null));

        verify(lamalAffiliationRepository, never()).save(any());
    }

    @Test
    @DisplayName("GIVEN an active LAMal WHEN recording an exemption over it THEN it is refused and nothing is saved")
    void shouldRefuseExemptionOverActiveLamal() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.FR));
        when(lamalAffiliationRepository.loadForUpdate(PERSON_ID)).thenReturn(LamalAffiliationFixtures.loadedFor(
                PERSON_ID,
                List.of(ContractFixtures.existingContract(UUID.randomUUID(),
                        PERSON_ID,
                        JAN_2026.atStartOfDay(),
                        null,
                        new BigDecimal("300.00"),
                        NOW)),
                null));

        assertThatThrownBy(() -> handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2027, null)))
                .isInstanceOf(LamalExemptionConflictException.class);
        verify(lamalAffiliationRepository, never()).save(any());
    }

    @Test
    @DisplayName("GIVEN a person living in Switzerland WHEN recording an exemption THEN it is refused before loading the affiliation")
    void shouldRefuseResident() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.CH));

        assertThatThrownBy(() -> handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, null)))
                .isInstanceOf(LamalExemptionNotAllowedException.class);
        verifyNoInteractions(lamalAffiliationRepository);
    }

    @Test
    @DisplayName("GIVEN a company WHEN recording an exemption THEN it is refused before loading the affiliation")
    void shouldRefuseCompany() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingCompany(PERSON_ID));

        assertThatThrownBy(() -> handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, null)))
                .isInstanceOf(LamalExemptionNotAllowedException.class);
        verifyNoInteractions(lamalAffiliationRepository);
    }

    @Test
    @DisplayName("GIVEN an end date before the start date WHEN recording an exemption THEN it is refused")
    void shouldRefuseInvalidPeriod() {
        when(clientRepository.getById(PERSON_ID)).thenReturn(ClientFixtures.existingPerson(PERSON_ID, Residence.FR));

        assertThatThrownBy(() -> handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2027, JAN_2026)))
                .isInstanceOf(InvalidLamalExemptionPeriodException.class);
        verifyNoInteractions(lamalAffiliationRepository);
    }

    @Test
    @DisplayName("GIVEN an unknown or deleted client WHEN recording an exemption THEN ClientNotFoundException")
    void shouldRefuseUnknownClient() {
        when(clientRepository.getById(PERSON_ID)).thenThrow(ClientNotFoundException.forId(PERSON_ID));

        assertThatThrownBy(() -> handler.execute(new RecordLamalExemption.Command(PERSON_ID, JAN_2026, null)))
                .isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(lamalAffiliationRepository);
    }
}
