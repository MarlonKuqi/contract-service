package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobQueryPort;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportContractImportJobRejections Handler")
class ExportContractImportJobRejectionsTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final int PAGE_SIZE = ExportContractImportJobRejections.PAGE_SIZE;

    @Mock
    private ContractImportJobRepository jobRepository;

    @Mock
    private ContractImportJobQueryPort queryPort;

    private static ContractImportRejection rejected(final int lineNumber, final ContractImportRejectionReason reason) {
        return ContractImportRejection.reconstituteFromDatabase(lineNumber,
                CLIENT_ID,
                reason,
                "reason of line " + lineNumber);
    }

    @Test
    @DisplayName("GIVEN rejections spread over several pages WHEN exporting THEN each comes with its original line, in file order")
    void shouldExportRejectionsWithTheirOriginalLines() {
        final String file = "clientId;startDate;endDate;costAmount\n" + CLIENT_ID + ";;;100.00\n" + CLIENT_ID + ";;;-1\n" + "\n" + "garbage;\"quoted\"\n";
        when(jobRepository.loadFile(JOB_ID)).thenReturn(file.getBytes(StandardCharsets.UTF_8));
        when(queryPort.findRejections(JOB_ID, PageRequest.of(0, PAGE_SIZE)))
                .thenReturn(new PageImpl<>(List.of(rejected(3, ContractImportRejectionReason.INVALID_COST)),
                        PageRequest.of(0, PAGE_SIZE),
                        PAGE_SIZE + 1L));
        when(queryPort.findRejections(JOB_ID, PageRequest.of(1, PAGE_SIZE)))
                .thenReturn(new PageImpl<>(List.of(rejected(5, ContractImportRejectionReason.INVALID_LINE_FORMAT)),
                        PageRequest.of(1, PAGE_SIZE),
                        PAGE_SIZE + 1L));

        final List<ExportContractImportJobRejections.RejectedLine> lines = new ExportContractImportJobRejections.Handler(
                jobRepository,
                queryPort)
                .execute(new ExportContractImportJobRejections.Query(JOB_ID));

        assertThat(lines)
                .extracting(ExportContractImportJobRejections.RejectedLine::lineNumber,
                        ExportContractImportJobRejections.RejectedLine::reason,
                        ExportContractImportJobRejections.RejectedLine::message,
                        ExportContractImportJobRejections.RejectedLine::line)
                .containsExactly(
                        tuple(3, ContractImportRejectionReason.INVALID_COST, "reason of line 3", CLIENT_ID + ";;;-1"),
                        tuple(5,
                                ContractImportRejectionReason.INVALID_LINE_FORMAT,
                                "reason of line 5",
                                "garbage;\"quoted\"")
                );
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN exporting THEN it is not found and its file is never read")
    void shouldNotReadFileOfUnknownJob() {
        doThrow(new ContractImportJobNotFoundException(JOB_ID)).when(jobRepository).ensureExists(JOB_ID);

        assertThatThrownBy(() -> new ExportContractImportJobRejections.Handler(jobRepository, queryPort)
                .execute(new ExportContractImportJobRejections.Query(JOB_ID)))
                .isInstanceOf(ContractImportJobNotFoundException.class);
        verify(jobRepository, never()).loadFile(any());
    }
}
