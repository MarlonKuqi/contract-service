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
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.features.contract.CreateContract;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

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

    @Autowired
    private ContractOverlapPolicy contractOverlapPolicy;

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
    @DisplayName("GIVEN no open transaction WHEN starting an overlap batch THEN it is refused, the client lock being released at once otherwise")
    void shouldRefuseOverlapBatchOutsideATransaction() {
        LocalDateTime now = LocalDateTime.now(clock);
        Client person = clientRepository.save(Person.of(
                ClientName.of("Lamal Without Transaction"),
                ClientEmail.of(TestDataHelper.uniqueEmail("lamal.without.transaction")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        Set<UUID> clientIds = Set.of(Objects.requireNonNull(person.id()));

        assertThatThrownBy(() -> contractOverlapPolicy.startBatch(clientIds, now))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("GIVEN no open transaction WHEN starting an overlap batch for no client THEN it is refused all the same, the missing transaction being a caller error")
    void shouldRefuseEmptyOverlapBatchOutsideATransaction() {
        LocalDateTime now = LocalDateTime.now(clock);

        assertThatThrownBy(() -> contractOverlapPolicy.startBatch(Set.of(), now))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("GIVEN no open transaction WHEN locking the clients of an import chunk THEN it is refused")
    void shouldRefuseChunkLockOutsideATransaction() {
        assertThatThrownBy(() -> clientRepository.lockExistingIds(Set.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private boolean aTransactionWaitsForALock() {
        Integer waiting = jdbcTemplate.queryForObject(
                "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and datname = current_database()",
                Integer.class);
        return waiting != null && waiting > 0;
    }
}
