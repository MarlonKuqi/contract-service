package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.ETags;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
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
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaEntity;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract cost update conflicts - Integration Tests")
class ContractCostConflictAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @MockitoSpyBean
    private ContractRepository contractRepository;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private LocalDateTime now;
    private Client client;
    private Contract contract;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);

        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();

        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        client = clientRepository.save(Person.of(
                ClientName.of("Cost Conflict"),
                ClientEmail.of(TestDataHelper.uniqueEmail("cost.conflict")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        contract = contractRepository.save(ContractFactory.createFromCommand(
                client.getId(), now.minusDays(1), null, new BigDecimal("1000.00"), now));
    }

    @Test
    @DisplayName("GIVEN another counselor changes the cost between the read and the write WHEN the cost is patched THEN the retry rejects it with 412 and keeps the concurrent cost")
    void shouldRejectRetriedUpdateWhenAnotherCostWasCommittedConcurrently() {
        injectConcurrentWriteAfterFirstRead(id -> {
            ContractJpaEntity entity = contractJpaRepository.findById(id).orElseThrow();
            entity.setCostAmount(new BigDecimal("1200.00"));
        });

        given()
                .header(HttpHeaders.IF_MATCH, ETags.ofVersion(0))
                .body("""
                        {
                            "amount": "1500.00"
                        }
                        """)
                .patch(ContractEndpoints.CONTRACT_COST, contract.getId())
                .then()
                .statusCode(412)
                .body("code", equalTo("preconditionFailed"));

        assertThat(persistedContract().getCostAmount()).isEqualByComparingTo("1200.00");
        verify(contractRepository, times(2)).getById(contract.getId());
    }

    @Test
    @DisplayName("GIVEN the contract is closed between the read and the write WHEN the cost is patched THEN the retry rejects it with 412 and the contract stays closed")
    void shouldRejectRetriedUpdateWhenContractWasClosedConcurrently() {
        injectConcurrentWriteAfterFirstRead(id -> contractRepository.closeAllActiveByClientId(client.getId(), now));

        given()
                .header(HttpHeaders.IF_MATCH, ETags.ofVersion(0))
                .body("""
                        {
                            "amount": "1500.00"
                        }
                        """)
                .patch(ContractEndpoints.CONTRACT_COST, contract.getId())
                .then()
                .statusCode(412)
                .body("code", equalTo("preconditionFailed"));

        ContractJpaEntity persisted = persistedContract();
        assertThat(persisted.getEndDate()).isEqualTo(now);
        assertThat(persisted.getCostAmount()).isEqualByComparingTo("1000.00");
    }

    private void injectConcurrentWriteAfterFirstRead(final Consumer<UUID> concurrentWrite) {
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicBoolean concurrentWriteDone = new AtomicBoolean(false);

        doAnswer(invocation -> {
            Object staleRead = invocation.callRealMethod();
            if (concurrentWriteDone.compareAndSet(false, true)) {
                concurrentTransaction.executeWithoutResult(status -> concurrentWrite.accept(invocation.getArgument(0)));
            }
            return staleRead;
        }).when(contractRepository).getById(any());
    }

    private ContractJpaEntity persistedContract() {
        return contractJpaRepository.findById(contract.getId()).orElseThrow();
    }
}
