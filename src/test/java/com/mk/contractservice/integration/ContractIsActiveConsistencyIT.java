package com.mk.contractservice.integration;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.infrastructure.persistence.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.ContractJpaRepository;
import com.mk.contractservice.integration.config.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract isActive() Consistency Tests")
class ContractIsActiveConsistencyIT {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    private Client testClient;

    @BeforeEach
    void setUp() {
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        testClient = Person.builder()
                .name(ClientName.of("Consistency Test Client"))
                .email(Email.of("consistency-" + System.currentTimeMillis() + "@test.com"))
                .phone(PhoneNumber.of("+33999999999"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                .build();
        testClient = clientRepository.save(testClient);
    }

    @Test
    @DisplayName("GIVEN contracts with various endDates WHEN checking isActive THEN domain and infrastructure agree")
    void shouldHaveConsistentActiveLogicBetweenDomainAndInfrastructure() {
        LocalDateTime now = LocalDateTime.now();

        Contract activeNoEndDate = createAndSaveContract(now.minusDays(10), null, "1000");
        Contract activeFutureEnd = createAndSaveContract(now.minusDays(10), now.plusDays(30), "2000");
        Contract expiredYesterday = createAndSaveContract(now.minusDays(100), now.minusDays(1), "3000");
        Contract expiredLastMonth = createAndSaveContract(now.minusDays(60), now.minusDays(30), "4000");

        Contract reloadedActiveNoEndDate = contractRepository.findById(activeNoEndDate.getId()).orElseThrow();
        Contract reloadedActiveFutureEnd = contractRepository.findById(activeFutureEnd.getId()).orElseThrow();
        Contract reloadedExpiredYesterday = contractRepository.findById(expiredYesterday.getId()).orElseThrow();
        Contract reloadedExpiredLastMonth = contractRepository.findById(expiredLastMonth.getId()).orElseThrow();

        assertThat(reloadedActiveNoEndDate.isActive()).isTrue();
        assertThat(reloadedActiveFutureEnd.isActive()).isTrue();
        assertThat(reloadedExpiredYesterday.isActive()).isFalse();
        assertThat(reloadedExpiredLastMonth.isActive()).isFalse();

        Page<Contract> activeFromInfra = contractRepository.findActiveByClientIdPageable(
                testClient.getId(),
                now,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(activeFromInfra.getTotalElements()).isEqualTo(2);
        assertThat(activeFromInfra.getContent())
                .extracting(Contract::getId)
                .containsExactlyInAnyOrder(
                        activeNoEndDate.getId(),
                        activeFutureEnd.getId()
                )
                .doesNotContain(
                        expiredYesterday.getId(),
                        expiredLastMonth.getId()
                );

        activeFromInfra.getContent().forEach(contract ->
                assertThat(contract.isActive())
                        .as("Contract %s returned by infrastructure query should be active", contract.getId())
                        .isTrue()
        );
    }

    @Test
    @DisplayName("GIVEN mix of active and expired contracts WHEN summing via infrastructure THEN only active contracts are summed")
    void shouldSumOnlyActiveContractsConsistentlyWithDomainLogic() {
        LocalDateTime now = LocalDateTime.now();

        Contract active1 = createAndSaveContract(now.minusDays(10), null, "1000");
        Contract active2 = createAndSaveContract(now.minusDays(5), now.plusDays(30), "2000");
        Contract expired1 = createAndSaveContract(now.minusDays(100), now.minusDays(1), "500");
        Contract expired2 = createAndSaveContract(now.minusDays(60), now.minusDays(30), "700");

        BigDecimal sumFromInfra = contractRepository.sumActiveByClientId(testClient.getId(), now);

        assertThat(sumFromInfra).isEqualByComparingTo(new BigDecimal("3000"));

        Contract reloadedActive1 = contractRepository.findById(active1.getId()).orElseThrow();
        Contract reloadedActive2 = contractRepository.findById(active2.getId()).orElseThrow();
        Contract reloadedExpired1 = contractRepository.findById(expired1.getId()).orElseThrow();
        Contract reloadedExpired2 = contractRepository.findById(expired2.getId()).orElseThrow();

        assertThat(reloadedActive1.isActive()).isTrue();
        assertThat(reloadedActive2.isActive()).isTrue();
        assertThat(reloadedExpired1.isActive()).isFalse();
        assertThat(reloadedExpired2.isActive()).isFalse();

        BigDecimal manualSum = BigDecimal.ZERO;
        if (reloadedActive1.isActive()) manualSum = manualSum.add(reloadedActive1.getCostAmount().value());
        if (reloadedActive2.isActive()) manualSum = manualSum.add(reloadedActive2.getCostAmount().value());
        if (reloadedExpired1.isActive()) manualSum = manualSum.add(reloadedExpired1.getCostAmount().value());
        if (reloadedExpired2.isActive()) manualSum = manualSum.add(reloadedExpired2.getCostAmount().value());

        assertThat(sumFromInfra).isEqualByComparingTo(manualSum);
    }

    private Contract createAndSaveContract(LocalDateTime startDate, LocalDateTime endDate, String amount) {
        Contract contract = Contract.builder()
                .client(testClient)
                .period(ContractPeriod.of(startDate, endDate))
                .costAmount(ContractCost.of(new BigDecimal(amount)))
                .build();
        return contractRepository.save(contract);
    }
}

