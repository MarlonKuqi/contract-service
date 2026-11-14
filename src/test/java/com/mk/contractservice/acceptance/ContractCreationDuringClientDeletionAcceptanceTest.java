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
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.features.client.DeleteClient;
import com.mk.contractservice.features.contract.CreateContract;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Technical guarantee: a client deleted during a contract creation never keeps an active contract")
class ContractCreationDuringClientDeletionAcceptanceTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private CreateContract createContract;

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

    private UUID createPerson() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final Client client = clientRepository.save(Person.of(
                ClientName.of("Creation Race"),
                ClientEmail.of(TestDataHelper.uniqueEmail("creation.race")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        return client.id();
    }

    private CreateContract.Command lamalFor(final UUID clientId) {
        final LocalDateTime now = LocalDateTime.now(clock);
        return new CreateContract.Command(clientId, ContractType.LAMAL, 300, now, null, new BigDecimal("300.00"));
    }

    private void awaitAnotherTransactionBlockedOnALock() {
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbcTemplate.queryForObject("""
                select count(*) from pg_stat_activity
                where datname = current_database() and wait_event_type = 'Lock'
                """, Long.class) > 0);
    }

    @Test
    @DisplayName("GIVEN a contract creation not yet committed WHEN the client is deleted THEN the deletion waits and closes the new contract")
    void shouldCloseContractCreatedBeforeTheDeletionCommits() throws Exception {
        final UUID clientId = createPerson();
        final AtomicReference<CompletableFuture<Void>> deletion = new AtomicReference<>();

        final Contract created = newTransaction().execute(status -> {
            final Contract contract = createContract.execute(lamalFor(clientId));
            deletion.set(CompletableFuture.runAsync(() -> deleteClient.execute(new DeleteClient.Command(clientId))));
            awaitAnotherTransactionBlockedOnALock();
            return contract;
        });

        deletion.get().get(10, TimeUnit.SECONDS);

        assertThat(contractJpaRepository.findById(created.id()).orElseThrow().getEndDate()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN a client deletion not yet committed WHEN a contract is created for it THEN the creation waits and is refused")
    void shouldRefuseContractForClientDeletedWhileTheCreationWaits() {
        final UUID clientId = createPerson();
        final AtomicReference<CompletableFuture<Contract>> creation = new AtomicReference<>();

        newTransaction().executeWithoutResult(status -> {
            clientRepository.delete(clientId, LocalDateTime.now(clock));
            creation.set(CompletableFuture.supplyAsync(() -> createContract.execute(lamalFor(clientId))));
            awaitAnotherTransactionBlockedOnALock();
        });

        assertThatThrownBy(() -> creation.get().get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ClientNotFoundException.class);
        assertThat(contractJpaRepository.findAll())
                .filteredOn(contract -> clientId.equals(contract.getClientId()))
                .isEmpty();
    }
}
