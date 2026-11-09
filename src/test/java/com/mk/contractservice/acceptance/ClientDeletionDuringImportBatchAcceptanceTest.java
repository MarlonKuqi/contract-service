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
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.features.client.DeleteClient;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Technical guarantee: a client deleted during an import batch never keeps an active contract")
class ClientDeletionDuringImportBatchAcceptanceTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private DeleteClient deleteClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate newTransaction() {
        final TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private UUID createClient() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final Client client = clientRepository.save(Person.of(
                ClientName.of("Import Race"),
                ClientEmail.of(TestDataHelper.uniqueEmail("import.race")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        return client.id();
    }

    private void awaitAnotherTransactionBlockedOnALock() {
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbcTemplate.queryForObject("""
                select count(*) from pg_stat_activity
                where datname = current_database() and wait_event_type = 'Lock'
                """, Long.class) > 0);
    }

    @Test
    @DisplayName(
        "GIVEN a batch holding the lock on its client WHEN the client is deleted THEN the contract of the batch is closed too")
    void shouldCloseContractInsertedByTheBatchHoldingTheLock() throws Exception {
        final UUID clientId = createClient();

        final AtomicReference<CompletableFuture<Void>> deletion = new AtomicReference<>();

        final Contract inserted = newTransaction().execute(status -> {
            assertThat(clientRepository.lockExistingIds(Set.of(clientId))).containsExactly(clientId);

            deletion.set(CompletableFuture.runAsync(
                    () -> deleteClient.execute(new DeleteClient.Command(clientId))));
            awaitAnotherTransactionBlockedOnALock();

            final LocalDateTime now = LocalDateTime.now(clock);
            return contractRepository.saveAll(List.of(ContractFixtures.newContract(
                    clientId,
                    now,
                    null,
                    new BigDecimal("100.00"),
                    now))).getFirst();
        });

        deletion.get().get(10, TimeUnit.SECONDS);

        assertThat(contractJpaRepository.findById(inserted.id()).orElseThrow().getEndDate()).isNotNull();
    }

    @Test
    @DisplayName(
        "GIVEN a deletion not yet committed WHEN a batch locks the client THEN it waits and no longer sees the client")
    void shouldNotSeeClientDeletedWhileWaitingForTheLock() throws Exception {
        final UUID clientId = createClient();
        final LocalDateTime now = LocalDateTime.now(clock);
        final AtomicReference<CompletableFuture<Set<UUID>>> lockedIds = new AtomicReference<>();

        newTransaction().executeWithoutResult(status -> {
            clientRepository.delete(clientId, now);
            lockedIds.set(CompletableFuture.supplyAsync(() -> newTransaction().execute(
                    batch -> clientRepository.lockExistingIds(Set.of(clientId)))));
            awaitAnotherTransactionBlockedOnALock();
        });

        assertThat(lockedIds.get().get(10, TimeUnit.SECONDS)).isEmpty();
    }
}
