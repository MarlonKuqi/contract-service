package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobQueryPort;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import com.mk.contractservice.domain.shared.UseCase;
import com.mk.contractservice.features.shared.ContractImportCsv;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ExportContractImportJobRejections {

    int PAGE_SIZE = 1000;

    record Query(UUID jobId) {
        public Query {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
        }
    }

    record RejectedLine(int lineNumber, ContractImportRejectionReason reason, String message, String line) {
    }

    List<RejectedLine> execute(Query query);

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements ExportContractImportJobRejections {

        private final ContractImportJobRepository jobRepository;
        private final ContractImportJobQueryPort queryPort;

        @Override
        public List<RejectedLine> execute(final Query query) {
            jobRepository.ensureExists(query.jobId());
            final List<ContractImportRejection> rejections = allRejections(query.jobId());
            final Set<Integer> lineNumbers = rejections.stream()
                    .map(ContractImportRejection::lineNumber)
                    .collect(Collectors.toSet());
            final Map<Integer, String> originalLines = ContractImportCsv.linesAt(jobRepository.loadFile(query.jobId()),
                    lineNumbers);
            return rejections.stream()
                    .map(rejection -> new RejectedLine(
                            rejection.lineNumber(),
                            rejection.reason(),
                            rejection.message(),
                            originalLines.getOrDefault(rejection.lineNumber(), "")))
                    .toList();
        }

        private List<ContractImportRejection> allRejections(final UUID jobId) {
            Page<ContractImportRejection> page = queryPort.findRejections(jobId, PageRequest.of(0, PAGE_SIZE));
            final List<ContractImportRejection> rejections = new ArrayList<>(page.getContent());
            while (page.hasNext()) {
                page = queryPort.findRejections(jobId, page.nextPageable());
                rejections.addAll(page.getContent());
            }
            return rejections;
        }
    }
}
