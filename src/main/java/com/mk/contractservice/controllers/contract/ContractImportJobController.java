package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractImportJobResponse;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.controllers.contract.shared.PagedContractImportRejectionResponse;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.features.contract.importjob.GetContractImportJob;
import com.mk.contractservice.features.contract.importjob.ListContractImportJobRejections;
import com.mk.contractservice.features.contract.importjob.SubmitContractImportJob;
import com.mk.contractservice.features.shared.ContractImportCsv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACT_IMPORT_JOBS)
@RequiredArgsConstructor
public class ContractImportJobController {

    private static final String DEFAULT_FILE_NAME = "import.csv";

    private final SubmitContractImportJob submitContractImportJob;
    private final GetContractImportJob getContractImportJob;
    private final ListContractImportJobRejections listContractImportJobRejections;

    @Operation(
            summary = "Submit a CSV file of contracts for asynchronous import",
            description = "UTF-8 file, separator ';', first line: " + ContractImportCsv.HEADER + ". Dates in ISO format, "
                    + "decimals with a point, startDate and endDate can stay empty. At most " + SubmitContractImportJob.MAX_LINES
                    + " contracts and 50 MB. Valid lines create their contract, the others are reported: "
                    + "a job can therefore end with some contracts imported and some lines rejected. "
                    + "Follow the job through the Location header."
    )
    @ApiResponse(
            responseCode = "202",
            description = "The file is stored and will be processed",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractImportJobResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Code missingParameter: the file part is missing",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "413",
            description = "Code importFileTooLarge: the file exceeds 50 MB",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Code invalidImportFile: no data, wrong header, encoding other than UTF-8, or too many lines. Nothing is stored",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractImportJobResponse> submit(@RequestParam("file") final MultipartFile file) throws IOException {
        final ContractImportJob job = submitContractImportJob.execute(new SubmitContractImportJob.Command(
                Optional.ofNullable(file.getOriginalFilename()).orElse(DEFAULT_FILE_NAME),
                file.getBytes()
        ));

        final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{jobId}")
                .buildAndExpand(job.getId())
                .toUri();
        return ResponseEntity.accepted().location(location).body(ContractImportJobResponse.from(job));
    }

    @Operation(summary = "Follow an asynchronous import job")
    @ApiResponse(
            responseCode = "200",
            description = "Current state of the job",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractImportJobResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Code contractImportJobNotFound",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.RELATIVE_CONTRACT_IMPORT_JOB)
    public ContractImportJobResponse get(@PathVariable(ContractEndpoints.JOB_ID) final UUID jobId) {
        return ContractImportJobResponse.from(getContractImportJob.execute(new GetContractImportJob.Query(jobId)));
    }

    @Operation(
            summary = "List the lines rejected by an import job",
            description = "Ordered by line number. Page size: at most " + ListContractImportJobRejections.MAX_PAGE_SIZE + "."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rejected lines known so far",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedContractImportRejectionResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Code contractImportJobNotFound",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.RELATIVE_CONTRACT_IMPORT_JOB_REJECTIONS)
    public PagedContractImportRejectionResponse listRejections(
            @PathVariable(ContractEndpoints.JOB_ID) final UUID jobId,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        return PagedContractImportRejectionResponse.from(
                listContractImportJobRejections.execute(new ListContractImportJobRejections.Query(jobId, page, size)));
    }
}
