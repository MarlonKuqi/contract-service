package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.features.contract.CreateContract;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("LAMal overlap concurrency - Integration Tests")
class LamalOverlapConcurrencyAcceptanceTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private CreateContract createContract;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private LamalAffiliationRepository lamalAffiliationRepository;

    @Test
    @DisplayName("GIVEN two counselors creating overlapping LAMal for a person at the same time WHEN the first one commits THEN the second one waits, sees it and is refused")
    void shouldRefuseOverlappingLamalCreatedConcurrently() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        Client person = clientRepository.save(Person.of(
                ClientName.of("Concurrent Lamal"),
                ClientEmail.of(TestDataHelper.uniqueEmail("concurrent.lamal")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        CreateContract.Command lamal2027 = new CreateContract.Command(person.id(),
                ContractType.LAMAL,
                300,
                now.plusYears(1),
                now.plusYears(2),
                new BigDecimal("300.00"));
        CountDownLatch firstCreated = new CountDownLatch(1);

        CompletableFuture<Void> firstCounselor = CompletableFuture.runAsync(() -> new TransactionTemplate(
                transactionManager).executeWithoutResult(status -> {
                    createContract.execute(lamal2027);
                    firstCreated.countDown();
                    await().atMost(Duration.ofSeconds(4)).until(this::aTransactionWaitsForALock);
                }));
        assertThat(firstCreated.await(10, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<Void> secondCounselor = CompletableFuture.runAsync(() -> createContract.execute(lamal2027));

        firstCounselor.get(10, TimeUnit.SECONDS);
        assertThatThrownBy(() -> secondCounselor.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(OverlappingContractException.class);
        assertThat(contractJpaRepository.findAll())
                .filteredOn(contract -> person.id().equals(contract.getClientId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("GIVEN a person with a LAMal WHEN another counselor commits an overlapping LAMal after the creation read the affiliation THEN the creation is retried, sees it and is refused")
    void shouldRetryAndRefuseLamalWhenAffiliationChangedConcurrently() {
        LocalDateTime now = LocalDateTime.now(clock);
        Client person = clientRepository.save(Person.of(
                ClientName.of("Concurrent Affiliation"),
                ClientEmail.of(TestDataHelper.uniqueEmail("concurrent.affiliation")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        createContract.execute(new CreateContract.Command(person.id(),
                ContractType.LAMAL,
                300,
                now,
                now.plusYears(1),
                new BigDecimal("300.00")));
        CreateContract.Command lamalNextYear = new CreateContract.Command(person.id(),
                ContractType.LAMAL,
                300,
                now.plusYears(1),
                now.plusYears(2),
                new BigDecimal("300.00"));
        AtomicBoolean concurrentCreationDone = new AtomicBoolean(false);
        doAnswer(invocation -> {
            Object staleRead = invocation.callRealMethod();
            if (concurrentCreationDone.compareAndSet(false, true)) {
                TransactionTemplate otherCounselor = new TransactionTemplate(transactionManager);
                otherCounselor.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                otherCounselor.executeWithoutResult(status -> createContract.execute(lamalNextYear));
            }
            return staleRead;
        }).when(AopTestUtils.<LamalAffiliationRepository>getUltimateTargetObject(lamalAffiliationRepository))
                .loadForAdmission(anySet());

        assertThatThrownBy(() -> createContract.execute(lamalNextYear))
                .isInstanceOf(OverlappingContractException.class);
        assertThat(contractJpaRepository.findAll())
                .filteredOn(contract -> person.id().equals(contract.getClientId()))
                .hasSize(2);
    }

    private boolean aTransactionWaitsForALock() {
        Integer waiting = jdbcTemplate.queryForObject(
                "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and datname = current_database()",
                Integer.class);
        return waiting != null && waiting > 0;
    }
}
