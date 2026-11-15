package com.mk.contractservice.features.lamal;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotFoundException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetLamalExemption")
class GetLamalExemptionTest {

    private static final UUID PERSON_ID = UUID.randomUUID();

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LamalAffiliationRepository lamalAffiliationRepository;

    private GetLamalExemption.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new GetLamalExemption.Handler(clientRepository, lamalAffiliationRepository);
    }

    @Test
    @DisplayName("GIVEN an exempted person WHEN reading the exemption THEN it is returned")
    void shouldReturnExemption() {
        LamalExemption exemption = LamalExemption.reconstituteFromDatabase(LocalDate.of(2026, 1, 1), null);
        when(lamalAffiliationRepository.findExemption(PERSON_ID)).thenReturn(Optional.of(exemption));

        assertThat(handler.execute(new GetLamalExemption.Query(PERSON_ID))).isEqualTo(exemption);
    }

    @Test
    @DisplayName("GIVEN a client without exemption WHEN reading the exemption THEN LamalExemptionNotFoundException")
    void shouldRefuseMissingExemption() {
        when(lamalAffiliationRepository.findExemption(PERSON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(new GetLamalExemption.Query(PERSON_ID)))
                .isInstanceOf(LamalExemptionNotFoundException.class)
                .hasMessage("No LAMal exemption recorded for client " + PERSON_ID);
    }

    @Test
    @DisplayName("GIVEN an unknown or deleted client WHEN reading the exemption THEN ClientNotFoundException")
    void shouldRefuseUnknownClient() {
        doThrow(ClientNotFoundException.forId(PERSON_ID)).when(clientRepository).ensureExists(PERSON_ID);

        assertThatThrownBy(() -> handler.execute(new GetLamalExemption.Query(PERSON_ID)))
                .isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(lamalAffiliationRepository);
    }
}
