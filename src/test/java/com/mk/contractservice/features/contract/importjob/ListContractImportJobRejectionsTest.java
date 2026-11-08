package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobQueryPort;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobNotFoundException;
import com.mk.contractservice.features.shared.InvalidPageRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListContractImportJobRejections Handler")
class ListContractImportJobRejectionsTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private ContractImportJobRepository jobRepository;

    @Mock
    private ContractImportJobQueryPort queryPort;

    private ListContractImportJobRejections handler;

    @BeforeEach
    void setUp() {
        handler = new ListContractImportJobRejections.Handler(jobRepository, queryPort);
    }

    @Test
    @DisplayName("GIVEN an existing job WHEN listing THEN the requested page of rejections is returned")
    void shouldReturnRequestedPage() {
        final Page<ContractImportRejection> page = new PageImpl<>(List.of(ContractImportRejection
                .reconstituteFromDatabase(
                        3,
                        null,
                        ContractImportRejectionReason.INVALID_LINE_FORMAT,
                        "Unreadable")));
        when(queryPort.findRejections(JOB_ID, PageRequest.of(2, 10))).thenReturn(page);

        assertThat(handler.execute(new ListContractImportJobRejections.Query(JOB_ID, 2, 10))).isSameAs(page);

    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN listing THEN it is not found and no rejection is read")
    void shouldNotReadRejectionsOfUnknownJob() {
        doThrow(new ContractImportJobNotFoundException(JOB_ID)).when(jobRepository).ensureExists(JOB_ID);
        final ListContractImportJobRejections.Query query = new ListContractImportJobRejections.Query(JOB_ID, 0, 20);

        assertThatThrownBy(() -> handler.execute(query)).isInstanceOf(ContractImportJobNotFoundException.class);

        verify(queryPort, never()).findRejections(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @DisplayName("GIVEN page or size out of bounds WHEN building Query THEN it is rejected")
    void shouldRejectOutOfBounds(final int page, final int size) {
        assertThatThrownBy(() -> new ListContractImportJobRejections.Query(JOB_ID, page, size))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    @DisplayName("GIVEN the maximum page size WHEN building Query THEN it is accepted")
    void shouldAcceptMaximumPageSize() {
        assertThatCode(() -> new ListContractImportJobRejections.Query(JOB_ID,
                0,
                ListContractImportJobRejections.MAX_PAGE_SIZE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN a null job id WHEN building Query THEN it is rejected")
    void shouldRejectNullJobId() {
        assertThatThrownBy(() -> new ListContractImportJobRejections.Query(null, 0, 20)).isInstanceOf(
                NullPointerException.class);
    }
}
