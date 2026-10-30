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
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaEntity;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Contract closure concurrency - Integration Tests")
class ContractClosureConcurrencyAcceptanceTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("GIVEN a contract loaded before a concurrent closure WHEN the stale copy is written THEN it is rejected and the contract stays closed")
    void shouldRejectStaleWriteAfterConcurrentClosure() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        Client client = clientRepository.save(Person.of(
                ClientName.of("Concurrent Closure"),
                ClientEmail.of(TestDataHelper.uniqueEmail("concurrent.closure")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15), now.toLocalDate())
        ));
        Contract contract = contractRepository.save(ContractFactory.createFromCommand(
                client.getId(), now.minusDays(1), null, new BigDecimal("100.00"), now));

        TransactionTemplate costUpdateTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate closureTransaction = new TransactionTemplate(transactionManager);
        closureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> costUpdateTransaction.executeWithoutResult(status -> {
            ContractJpaEntity staleCopy = contractJpaRepository.findById(contract.getId()).orElseThrow();
            closureTransaction.executeWithoutResult(closure -> contractRepository.closeAllActiveByClientId(client.getId(), now));
            staleCopy.setCostAmount(new BigDecimal("200.00"));
        })).isInstanceOf(OptimisticLockingFailureException.class);

        ContractJpaEntity persisted = contractJpaRepository.findById(contract.getId()).orElseThrow();
        assertThat(persisted.getEndDate()).isEqualTo(now);
        assertThat(persisted.getCostAmount()).isEqualByComparingTo("100.00");
    }
}
