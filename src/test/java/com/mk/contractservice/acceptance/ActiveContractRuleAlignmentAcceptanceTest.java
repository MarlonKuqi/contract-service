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
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Active contract rule - domain and persistence alignment")
class ActiveContractRuleAlignmentAcceptanceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime PAST = NOW.minusMonths(6);
    private static final LocalDateTime FUTURE = NOW.plusMonths(6);
    private static final BigDecimal COST = new BigDecimal("150.00");

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractQueryPort contractQueryPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    static Stream<Arguments> boundaryCases() {
        return Stream.of(
                arguments("started, no end date", PAST, null, true),
                arguments("starts in the future, no end date", FUTURE, null, true),
                arguments("starts in the future, ends later", FUTURE, FUTURE.plusMonths(1), true),
                arguments("ends one microsecond after now", PAST, NOW.plus(1, ChronoUnit.MICROS), true),
                arguments("ends exactly now", PAST, NOW, false),
                arguments("ended one microsecond before now", PAST, NOW.minus(1, ChronoUnit.MICROS), false),
                arguments("ended in the past", PAST.minusMonths(6), PAST, false)
        );
    }

    @ParameterizedTest(name = "{0} -> active: {3}")
    @MethodSource("boundaryCases")
    @DisplayName("GIVEN a contract WHEN its activity is evaluated at now THEN the domain and every persistence query agree")
    void shouldEvaluateActivityIdenticallyInDomainAndPersistence(
            final String description,
            final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final boolean expectedActive
    ) {
        final Client client = clientRepository.save(Person.of(
                ClientName.of("Alignment Client"),
                ClientEmail.of(TestDataHelper.uniqueEmail("alignment")),
                ClientPhoneNumber.of(TestDataHelper.randomSwissPhoneNumber()),
                PersonBirthDate.of(LocalDate.of(1985, 3, 20), NOW.toLocalDate())
        ));
        final Contract contract = contractRepository.save(
                ContractFactory.createFromCommand(client.getId(), startDate, endDate, COST, PAST.minusYears(1))
        );

        final boolean activeInDomain = contract.isActiveAt(NOW);
        final boolean listedAsActive = !contractRepository
                .findByCriteria(ContractSearchCriteria.activeForClient(client.getId(), NOW), Pageable.unpaged())
                .isEmpty();
        final BigDecimal activeTotal = contractQueryPort.calculateTotalCostAmountForClient(client.getId(), NOW);
        final Integer closedCount = new TransactionTemplate(transactionManager)
                .execute(status -> contractRepository.closeAllActiveByClientId(client.getId(), NOW));

        assertThat(activeInDomain).as("domain").isEqualTo(expectedActive);
        assertThat(listedAsActive).as("active contracts listing").isEqualTo(expectedActive);
        assertThat(activeTotal).as("active contracts total").isEqualByComparingTo(expectedActive ? COST : BigDecimal.ZERO);
        assertThat(closedCount).as("bulk closure").isEqualTo(expectedActive ? 1 : 0);
    }
}
