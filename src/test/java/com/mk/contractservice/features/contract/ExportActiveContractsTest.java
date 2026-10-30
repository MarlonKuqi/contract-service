package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportActiveContracts Handler")
class ExportActiveContractsTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private ContractRepository contractRepository;

    private ExportActiveContracts.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new ExportActiveContracts.Handler(contractRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN a size below one WHEN building Query THEN throw IllegalArgumentException")
    void shouldRejectSizeBelowOne() {
        assertThatThrownBy(() -> new ExportActiveContracts.Query(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GIVEN more active contracts than the page size WHEN execute THEN return one page and continue after its last contract")
    void shouldReturnOnePageAndContinueAfterItsLastContract() {
        final List<Contract> fetched = contracts(3);
        when(contractRepository.findActiveAfter(NOW, null, 3)).thenReturn(fetched);

        final ExportActiveContracts.Result result = handler.execute(new ExportActiveContracts.Query(null, 2));

        assertThat(result.contracts()).containsExactly(fetched.get(0), fetched.get(1));
        assertThat(result.nextAfter()).contains(fetched.get(1).getId());
    }

    @Test
    @DisplayName("GIVEN exactly a page of active contracts left WHEN execute THEN return them as the last page")
    void shouldEndExportWhenExactlyOnePageIsLeft() {
        final UUID after = UUID.randomUUID();
        final List<Contract> fetched = contracts(2);
        when(contractRepository.findActiveAfter(NOW, after, 3)).thenReturn(fetched);

        final ExportActiveContracts.Result result = handler.execute(new ExportActiveContracts.Query(after, 2));

        assertThat(result.contracts()).isEqualTo(fetched);
        assertThat(result.nextAfter()).isEmpty();
    }

    private static List<Contract> contracts(final int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> ContractFactory.buildFromDatabase(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        NOW.minusMonths(1),
                        null,
                        new BigDecimal("100.00"),
                        NOW.minusMonths(1)
                ))
                .toList();
    }
}
