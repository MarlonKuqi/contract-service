package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.ContractTypeNotAllowedException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateContract Handler")
class CreateContractTest {

    private static final BigDecimal COST = new BigDecimal("1000.00");

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final UUID clientId = UUID.randomUUID();

    private CreateContract.Handler createContractHandler;

    @BeforeEach
    void setUp() {
        createContractHandler = new CreateContract.Handler(clientRepository,
                contractRepository,
                new ContractOverlapPolicy(contractRepository),
                clock);
    }

    private CreateContract.Command commandOf(final ContractType type, final BigDecimal cost) {
        return new CreateContract.Command(clientId, type, now, now.plusMonths(12), cost);
    }

    @Test
    @DisplayName("GIVEN une personne WHEN elle souscrit une LAMal THEN sauvegarde et retourne le contrat créé")
    void shouldSaveAndReturnCreatedContract() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingPerson(clientId));
        Contract saved = ContractFixtures.existingContract(UUID.randomUUID(),
                clientId,
                now,
                now.plusMonths(12),
                COST,
                now);
        when(contractRepository.save(any(Contract.class))).thenReturn(saved);

        Contract result = createContractHandler.execute(commandOf(ContractType.LAMAL, COST));

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().clientId()).isEqualTo(clientId);
        assertThat(captor.getValue().type()).isEqualTo(ContractType.LAMAL);
        assertThat(captor.getValue().lastModified()).isEqualTo(now);
    }

    @Test
    @DisplayName("GIVEN une entreprise WHEN elle souscrit une LPP THEN le contrat créé porte ce type")
    void shouldCreateLppContractForCompany() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingCompany(clientId));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract result = createContractHandler.execute(commandOf(ContractType.LPP, COST));

        assertThat(result.type()).isEqualTo(ContractType.LPP);
    }

    @Test
    @DisplayName("GIVEN une personne avec une LAMal qui couvre la période WHEN elle souscrit une LAMal THEN refuse sans créer de contrat")
    void shouldRefuseLamalOverlappingExistingOne() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingPerson(clientId));
        when(contractRepository.findAllByClientIdsAndTypes(Set.of(clientId), Set.of(ContractType.LAMAL)))
                .thenReturn(List.of(ContractFixtures.existingContract(UUID.randomUUID(),
                        clientId,
                        now.minusMonths(1),
                        null,
                        COST,
                        now)));
        CreateContract.Command command = commandOf(ContractType.LAMAL, COST);

        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(OverlappingContractException.class);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN une personne avec une LAMal qui finit au début de la nouvelle WHEN elle souscrit une LAMal THEN crée le contrat")
    void shouldCreateLamalStartingWhenPreviousOneEnds() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingPerson(clientId));
        when(contractRepository.findAllByClientIdsAndTypes(Set.of(clientId), Set.of(ContractType.LAMAL)))
                .thenReturn(List.of(ContractFixtures.existingContract(UUID.randomUUID(),
                        clientId,
                        now.minusYears(1),
                        now,
                        COST,
                        now)));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract result = createContractHandler.execute(commandOf(ContractType.LAMAL, COST));

        assertThat(result.period().startDate()).isEqualTo(now);
    }

    @Test
    @DisplayName("GIVEN une personne WHEN elle souscrit une LAMal THEN verrouille sa couverture avant de lire ses LAMal")
    void shouldLockCoverageBeforeReadingExistingLamal() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingPerson(clientId));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createContractHandler.execute(commandOf(ContractType.LAMAL, COST));

        InOrder inOrder = inOrder(contractRepository);
        inOrder.verify(contractRepository).lockCoverageOf(Set.of(clientId));
        inOrder.verify(contractRepository).findAllByClientIdsAndTypes(Set.of(clientId), Set.of(ContractType.LAMAL));
        inOrder.verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN une entreprise WHEN elle souscrit une LAA THEN ne cherche pas de chevauchement")
    void shouldNotLookForOverlapWhenTypeAllowsIt() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingCompany(clientId));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createContractHandler.execute(commandOf(ContractType.LAA, COST));

        verify(contractRepository, never()).findAllByClientIdsAndTypes(anySet(), anySet());
        verify(contractRepository, never()).lockCoverageOf(anySet());
    }

    @Test
    @DisplayName("GIVEN une entreprise WHEN elle souscrit une LAMal THEN refuse sans créer de contrat")
    void shouldRefuseTypeNotAllowedForClient() {
        when(clientRepository.getById(clientId)).thenReturn(ClientFixtures.existingCompany(clientId));
        CreateContract.Command command = commandOf(ContractType.LAMAL, COST);

        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(ContractTypeNotAllowedException.class);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN client inexistant WHEN execute THEN lève ClientNotFoundException sans créer de contrat")
    void shouldThrowExceptionWhenClientNotFound() {
        when(clientRepository.getById(clientId)).thenThrow(ClientNotFoundException.forId(clientId));
        CreateContract.Command command = commandOf(ContractType.LAMAL, COST);

        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(ClientNotFoundException.class);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN coût invalide WHEN execute THEN lève InvalidContractCostException sans interroger les clients")
    void shouldValidateContractBeforeCheckingClientExistence() {
        CreateContract.Command command = commandOf(ContractType.LAMAL, new BigDecimal("-1000.00"));

        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(InvalidContractCostException.class);
        verify(clientRepository, never()).getById(any(UUID.class));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN aucun type de contrat WHEN création de la commande THEN refuse la commande")
    void shouldRefuseCommandWithoutType() {
        assertThatThrownBy(() -> commandOf(null, COST))
                .isInstanceOf(NullPointerException.class);
    }
}
