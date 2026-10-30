package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Client update conflicts - Integration Tests")
class ClientConflictAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @MockitoSpyBean
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private LocalDateTime now;
    private Client client;
    private String concurrentPhone;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        client = clientRepository.save(Person.of(
                ClientName.of("Client Conflict"),
                ClientEmail.of(TestDataHelper.uniqueEmail("client.conflict")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        concurrentPhone = TestDataHelper.randomSwissPhoneNumber();
    }

    @Test
    @DisplayName("GIVEN another counselor changes the phone between the read and the write WHEN the name is patched THEN the update is retried and both changes are kept")
    void shouldRetryPatchAndKeepConcurrentChangeOnAnotherField() {
        injectConcurrentWriteAfterFirstRead(entity -> entity.setPhone(concurrentPhone));

        given()
                .body("""
                        {
                            "name": "Patched Name"
                        }
                        """)
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        ClientJpaEntity persisted = clientJpaRepository.findById(client.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Patched Name");
        assertThat(persisted.getPhone()).isEqualTo(concurrentPhone);
        verify(clientRepository, times(2)).getById(client.getId());
    }

    @Test
    @DisplayName("GIVEN the client is deleted between the read and the write WHEN the client is patched THEN the retry rejects it as not found")
    void shouldRejectRetriedPatchWhenClientWasDeletedConcurrently() {
        injectConcurrentWriteAfterFirstRead(entity -> entity.setDeletedAt(now));

        given()
                .body("""
                        {
                            "name": "Patched Name"
                        }
                        """)
                .patch(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(404);

        assertThat(clientJpaRepository.findById(client.getId())).isEmpty();
        verify(clientRepository, times(2)).getById(client.getId());
    }

    @Test
    @DisplayName("GIVEN another counselor changes the client between the read and the write WHEN the client is deleted THEN the deletion is retried and its contracts are closed")
    void shouldRetryDeletionWhenClientWasModifiedConcurrently() {
        Contract contract = contractRepository.save(ContractFactory.createFromCommand(
                client.getId(), now.minusDays(1), null, new BigDecimal("1000.00"), now));
        AtomicBoolean concurrentWriteDone = new AtomicBoolean(false);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (concurrentWriteDone.compareAndSet(false, true)) {
                writeConcurrently(invocation.getArgument(0), entity -> entity.setPhone(concurrentPhone));
            }
            return result;
        }).when(clientRepository).delete(any(), any());

        given()
                .delete(ClientEndpoints.CLIENT_BY_ID, client.getId())
                .then()
                .statusCode(204);

        assertThat(clientJpaRepository.findById(client.getId())).isEmpty();
        assertThat(contractJpaRepository.findById(contract.getId()).orElseThrow().getEndDate()).isNotNull();
        verify(clientRepository, times(2)).delete(any(), any());
    }

    private void injectConcurrentWriteAfterFirstRead(final Consumer<ClientJpaEntity> concurrentWrite) {
        AtomicBoolean concurrentWriteDone = new AtomicBoolean(false);
        doAnswer(invocation -> {
            Object staleRead = invocation.callRealMethod();
            if (concurrentWriteDone.compareAndSet(false, true)) {
                writeConcurrently(invocation.getArgument(0), concurrentWrite);
            }
            return staleRead;
        }).when(clientRepository).getById(any());
    }

    private void writeConcurrently(final UUID clientId, final Consumer<ClientJpaEntity> concurrentWrite) {
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        concurrentTransaction.executeWithoutResult(status ->
                concurrentWrite.accept(clientJpaRepository.findById(clientId).orElseThrow()));
    }
}
