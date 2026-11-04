package com.mk.contractservice.batch;

import com.mk.contractservice.features.contract.importjob.LoadContractImportJobFile;
import com.mk.contractservice.features.contract.importjob.ProcessContractImportBatch;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

@Configuration
public class ContractImportBatchConfiguration {

    public static final int CHUNK_SIZE = 2000;
    static final String JOB_NAME = "contractImportJob";
    static final String JOB_ID_PARAMETER = "jobId";

    private static final String STEP_NAME = "contractImportStep";
    private static final String READER_NAME = "contractImportReader";
    private static final String JOB_ID_EXPRESSION = "#{jobParameters['" + JOB_ID_PARAMETER + "']}";

    @Bean
    Job contractImportJob(
            final JobRepository jobRepository,
            final Step contractImportStep,
            final ContractImportJobOutcomeListener outcomeListener
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(outcomeListener)
                .start(contractImportStep)
                .build();
    }

    @Bean
    Step contractImportStep(
            final JobRepository jobRepository,
            final PlatformTransactionManager transactionManager,
            final FlatFileItemReader<RawLine> contractImportReader,
            final ItemProcessor<RawLine, RawLine> contractImportProcessor,
            final ItemWriter<RawLine> contractImportWriter,
            final ContractImportStepInterruptionPolicy interruptionPolicy
    ) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<RawLine, RawLine>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .interruptionPolicy(interruptionPolicy)
                .reader(contractImportReader)
                .processor(contractImportProcessor)
                .writer(contractImportWriter)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<RawLine> contractImportReader(
            @Value(JOB_ID_EXPRESSION) final String jobId,
            final LoadContractImportJobFile loadFile
    ) {
        final byte[] content = loadFile.execute(new LoadContractImportJobFile.Query(UUID.fromString(jobId)));
        return new FlatFileItemReaderBuilder<RawLine>()
                .name(READER_NAME)
                .resource(new ByteArrayResource(content))
                .encoding(StandardCharsets.UTF_8.name())
                .linesToSkip(1)
                .comments()
                .lineMapper((line, lineNumber) -> new RawLine(lineNumber, line))
                .build();
    }

    @Bean
    ItemProcessor<RawLine, RawLine> contractImportProcessor() {
        return line -> line.raw().isBlank() ? null : line;
    }

    @Bean
    @StepScope
    ItemWriter<RawLine> contractImportWriter(
            @Value(JOB_ID_EXPRESSION) final String jobId,
            final ProcessContractImportBatch processBatch
    ) {
        return chunk -> processBatch.execute(new ProcessContractImportBatch.Command(
                UUID.fromString(jobId),
                new ArrayList<>(chunk.getItems())
        ));
    }
}
