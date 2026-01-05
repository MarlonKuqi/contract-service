package com.mk.contractservice.integration.usecases;

import com.mk.contractservice.application.feature.contract.closeactive.CloseActiveContracts;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("UC: Close Active Contracts - Use Case Integration Tests (with real DB)")
class CloseActiveContractsUseCaseIT {

    @Autowired
    private CloseActiveContracts closeActiveContracts;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        // Créer un client de test en vraie BDD
        testClient = Person.of(
                ClientName.of("Client To Delete"),
                ClientEmail.of("delete.test." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234567"),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20))
        );
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("Should close ALL active contracts (endDate=null and endDate>now)")
    void shouldCloseAllActiveContracts() {
        // GIVEN: 3 contrats actifs + 2 déjà expirés
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closureDate = now;

        // Actif 1: endDate = null
        Contract active1 = createContract(
                testClient.getId(),
                now.minusDays(30),
                null,
                new BigDecimal("1000.00")
        );

        // Actif 2: endDate dans le futur
        Contract active2 = createContract(
                testClient.getId(),
                now.minusDays(10),
                now.plusMonths(6),
                new BigDecimal("2000.00")
        );

        // Actif 3: endDate dans le futur lointain
        Contract active3 = createContract(
                testClient.getId(),
                now.minusDays(5),
                now.plusYears(2),
                new BigDecimal("3000.00")
        );

        // Expiré 1: endDate hier
        Contract expired1 = createContract(
                testClient.getId(),
                now.minusMonths(6),
                now.minusDays(1),
                new BigDecimal("500.00")
        );

        // Expiré 2: endDate le mois dernier
        Contract expired2 = createContract(
                testClient.getId(),
                now.minusMonths(12),
                now.minusMonths(1),
                new BigDecimal("700.00")
        );

        closeActiveContracts.execute(new CloseActiveContracts.Command(testClient.getId(), closureDate));

        Contract closedActive1 = contractRepository.findById(active1.getId()).orElseThrow();
        assertThat(closedActive1.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);

        Contract closedActive2 = contractRepository.findById(active2.getId()).orElseThrow();
        assertThat(closedActive2.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);

        Contract closedActive3 = contractRepository.findById(active3.getId()).orElseThrow();
        assertThat(closedActive3.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);

        Contract unchangedExpired1 = contractRepository.findById(expired1.getId()).orElseThrow();
        assertThat(unchangedExpired1.getPeriod().getEndDate())
                .isEqualToIgnoringNanos(expired1.getPeriod().getEndDate());

        Contract unchangedExpired2 = contractRepository.findById(expired2.getId()).orElseThrow();
        assertThat(unchangedExpired2.getPeriod().getEndDate())
                .isEqualToIgnoringNanos(expired2.getPeriod().getEndDate());
    }

    @Test
    @DisplayName("Should handle client with NO contracts gracefully")
    void shouldHandleClientWithNoContracts() {
        closeActiveContracts.execute(new CloseActiveContracts.Command(testClient.getId(), LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle client with ONLY expired contracts")
    void shouldHandleClientWithOnlyExpiredContracts() {
        // GIVEN: Client avec seulement des contrats expirés
        LocalDateTime now = LocalDateTime.now();

        Contract expired1 = createContract(
                testClient.getId(),
                now.minusMonths(6),
                now.minusDays(10),
                new BigDecimal("1000.00")
        );

        Contract expired2 = createContract(
                testClient.getId(),
                now.minusMonths(12),
                now.minusMonths(1),
                new BigDecimal("2000.00")
        );

        LocalDateTime originalEndDate1 = expired1.getPeriod().getEndDate();
        LocalDateTime originalEndDate2 = expired2.getPeriod().getEndDate();

        // WHEN
        closeActiveContracts.execute(new CloseActiveContracts.Command(testClient.getId(), now));

        // THEN: Les contrats expirés ne doivent PAS changer
        Contract unchangedExpired1 = contractRepository.findById(expired1.getId()).orElseThrow();
        assertThat(unchangedExpired1.getPeriod().getEndDate())
                .isEqualToIgnoringNanos(originalEndDate1);

        Contract unchangedExpired2 = contractRepository.findById(expired2.getId()).orElseThrow();
        assertThat(unchangedExpired2.getPeriod().getEndDate())
                .isEqualToIgnoringNanos(originalEndDate2);
    }

    @Test
    @DisplayName("Should handle client with ONLY active contracts (endDate=null)")
    void shouldHandleClientWithOnlyNullEndDateContracts() {
        // GIVEN: Client avec seulement des contrats à durée indéterminée
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closureDate = now;

        Contract indefinite1 = createContract(
                testClient.getId(),
                now.minusMonths(3),
                null,
                new BigDecimal("1000.00")
        );

        Contract indefinite2 = createContract(
                testClient.getId(),
                now.minusMonths(1),
                null,
                new BigDecimal("2000.00")
        );

        // WHEN
        closeActiveContracts.execute(new CloseActiveContracts.Command(testClient.getId(), closureDate));

        // THEN: Tous doivent avoir endDate = closureDate
        Contract closed1 = contractRepository.findById(indefinite1.getId()).orElseThrow();
        assertThat(closed1.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);

        Contract closed2 = contractRepository.findById(indefinite2.getId()).orElseThrow();
        assertThat(closed2.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);
    }

    @Test
    @DisplayName("Should NOT affect contracts of other clients")
    void shouldNotAffectOtherClientsContracts() {
        // GIVEN: Deux clients avec des contrats
        Client otherClient = Person.of(
                ClientName.of("Other Client"),
                ClientEmail.of("other." + UUID.randomUUID().toString().substring(0, 8) + "@example.com"),
                ClientPhoneNumber.of("+41791234568"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        otherClient = clientRepository.save(otherClient);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closureDate = now;

        // Contrats du client à supprimer
        Contract testClientContract = createContract(
                testClient.getId(),
                now.minusDays(10),
                null,
                new BigDecimal("1000.00")
        );

        // Contrats de l'autre client
        Contract otherClientContract = createContract(
                otherClient.getId(),
                now.minusDays(10),
                null,
                new BigDecimal("2000.00")
        );

        // WHEN: Fermer les contrats du premier client
        closeActiveContracts.execute(new CloseActiveContracts.Command(testClient.getId(), closureDate));

        // THEN: Le contrat du premier client est fermé
        Contract closedContract = contractRepository.findById(testClientContract.getId()).orElseThrow();
        assertThat(closedContract.getPeriod().getEndDate())
                .isNotNull()
                .isEqualToIgnoringNanos(closureDate);

        // MAIS le contrat de l'autre client reste INCHANGÉ
        Contract unchangedContract = contractRepository.findById(otherClientContract.getId()).orElseThrow();
        assertThat(unchangedContract.getPeriod().getEndDate()).isNull();
    }

    private Contract createContract(UUID clientId, LocalDateTime startDate, LocalDateTime endDate, BigDecimal costAmount) {
        Contract contract = Contract.builder()
                .clientId(clientId)
                .period(ContractPeriod.of(startDate, endDate))
                .costAmount(ContractCost.of(costAmount))
                .build();

        return contractRepository.save(contract);
    }
}

