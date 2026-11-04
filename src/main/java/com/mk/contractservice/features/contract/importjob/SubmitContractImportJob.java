package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.exception.InvalidImportFileException;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.shared.UseCase;
import com.mk.contractservice.features.shared.ContractImportCsv;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public interface SubmitContractImportJob {

    int MAX_LINES = 500_000;
    int MAX_FILE_NAME_LENGTH = 255;

    record Command(String fileName, byte[] content, int totalLines) {

        public Command(final String fileName, final byte[] content) {
            this(fileName, content, ContractImportCsv.requireValidAndCountLines(Objects.requireNonNull(content, "Content cannot be null")));
        }

        public Command {
            Objects.requireNonNull(fileName, "File name cannot be null");
            if (fileName.length() > MAX_FILE_NAME_LENGTH) {
                throw new InvalidImportFileException("The file name must not exceed " + MAX_FILE_NAME_LENGTH + " characters");
            }
            if (totalLines > MAX_LINES) {
                throw new InvalidImportFileException("The file must not exceed " + MAX_LINES + " contracts, but had: " + totalLines);
            }
        }
    }

    ContractImportJob execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements SubmitContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public ContractImportJob execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            return jobRepository.insert(ContractImportJob.of(command.fileName(), command.totalLines(), now), command.content());
        }
    }
}
