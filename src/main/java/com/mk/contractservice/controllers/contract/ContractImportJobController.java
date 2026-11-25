package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractImportJobCancellationResponse;
import com.mk.contractservice.controllers.contract.shared.ContractImportJobResponse;
import com.mk.contractservice.controllers.contract.shared.ContractImportRejectionCsv;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.controllers.contract.shared.PagedContractImportJobResponse;
import com.mk.contractservice.controllers.contract.shared.PagedContractImportRejectionResponse;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.features.contract.importjob.CancelContractImportJob;
import com.mk.contractservice.features.contract.importjob.ContractImportFile;
import com.mk.contractservice.features.contract.importjob.ExportContractImportJobRejections;
import com.mk.contractservice.features.contract.importjob.GetContractImportJob;
import com.mk.contractservice.features.contract.importjob.ListContractImportJobRejections;
import com.mk.contractservice.features.contract.importjob.ListContractImportJobs;
import com.mk.contractservice.features.contract.importjob.SubmitContractImportJob;
import com.mk.contractservice.features.shared.ContractImportCsv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACT_IMPORT_JOBS)
@RequiredArgsConstructor
public class ContractImportJobController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String USERNAME_CLAIM = "preferred_username";
    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final SubmitContractImportJob submitContractImportJob;
    private final GetContractImportJob getContractImportJob;
    private final ListContractImportJobs listContractImportJobs;
    private final ListContractImportJobRejections listContractImportJobRejections;
    private final ExportContractImportJobRejections exportContractImportJobRejections;
    private final CancelContractImportJob cancelContractImportJob;

    @Operation(
            summary = "Submit a CSV file of contracts for asynchronous import",
            description = "UTF-8 file, separator ';', first line: " + ContractImportCsv.HEADER + ". type is LAMAL, LAA or LPP, dates in ISO format, " + "decimals with a point, startDate and endDate can stay empty. At most " + ContractImportFile.MAX_LINES + " contracts and 50 MB. Valid lines create their contract, the others are reported: " + "a job can therefore end with some contracts imported and some lines rejected. " + "Follow the job through the Location header. Send an Idempotency-Key to submit again safely " + "after a timeout: the same key and the same file return the first job instead of importing twice."
    )
    @ApiResponse(
            responseCode = "202",
            description = "The file is stored and will be processed, or the job already submitted with this Idempotency-Key",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractImportJobResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Code missingParameter: the file part is missing. Code invalidIdempotencyKey: blank or longer than " + IdempotencyKey.MAX_LENGTH + " characters",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Code dataIntegrityViolation: another submission with the same Idempotency-Key is being stored",
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
            description = "Code invalidImportFile: no data, wrong header, encoding other than UTF-8, or too many lines. " + "Code idempotencyKeyReused: this Idempotency-Key was already used for another file. Nothing is stored",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractImportJobResponse> submit(
            @RequestParam("file") final MultipartFile file,
            @Parameter(
                    description = "Client-generated key, at most " + IdempotencyKey.MAX_LENGTH + " characters, unique per counselor") @RequestHeader(
                            name = IDEMPOTENCY_KEY, required = false) @Nullable final String idempotencyKey,
            @AuthenticationPrincipal final Jwt counselor
    ) throws IOException {
        final ContractImportJob job = submitContractImportJob.execute(new SubmitContractImportJob.Command(
                file.getOriginalFilename(),
                file.getBytes(),
                usernameOf(counselor),
                idempotencyKey
        ));
        final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{jobId}")
                .buildAndExpand(job.id())
                .toUri();
        return ResponseEntity.accepted().location(location).body(ContractImportJobResponse.from(job));
    }

    @Operation(
            summary = "List the import jobs, newest first",
            description = "Jobs of every counselor, or of one counselor with submittedBy. Page size: at most " + ListContractImportJobs.MAX_PAGE_SIZE + "."
    )
    @ApiResponse(
            responseCode = "200",
            description = "One page of import jobs",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedContractImportJobResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Code invalidParameter: page or size out of bounds",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping
    public PagedContractImportJobResponse list(
            @Parameter(description = "Username of the counselor who submitted the jobs",
                    example = "counselor.demo") @RequestParam(required = false) @Nullable final String submittedBy,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size
    ) {
        return PagedContractImportJobResponse.from(
                listContractImportJobs.execute(new ListContractImportJobs.Query(submittedBy, page, size)));
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
            summary = "Cancel an import job and close the contracts it created",
            description = "Works on a job in any state. The job stops at its next chunk and will not run again. " + "Its contracts still active are closed now (end date = cancellation date), like the contracts " + "of a deleted client: they leave the active list and the totals, and stay readable. " + "A fixed file can then be submitted without active duplicates. Cancelling again changes nothing."
    )
    @ApiResponse(
            responseCode = "200",
            description = "The cancelled job and the number of contracts closed by this call",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractImportJobCancellationResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Code contractImportJobNotFound",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(ContractEndpoints.RELATIVE_CONTRACT_IMPORT_JOB_CANCELLATION)
    public ContractImportJobCancellationResponse cancel(
            @PathVariable(ContractEndpoints.JOB_ID) final UUID jobId,
            @AuthenticationPrincipal final Jwt counselor
    ) {
        return ContractImportJobCancellationResponse.from(
                cancelContractImportJob.execute(new CancelContractImportJob.Command(jobId, usernameOf(counselor))));
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

    @Operation(
            summary = "Download the rejected lines of an import job as CSV",
            description = "UTF-8, separator ';', columns " + ContractImportRejectionCsv.HEADER + ", ordered by line number. " + "The line column holds the original line of the file: fix it, then copy it into a new file to submit. " + "Text columns are quoted, inner quotes doubled. " + "A text value starting with =, +, -, @, a tab or a carriage return is prefixed with an apostrophe so that a spreadsheet shows it as text: remove it before copying the line."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rejected lines known so far",
            content = @Content(mediaType = "text/csv")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Code contractImportJobNotFound",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.RELATIVE_CONTRACT_IMPORT_JOB_REJECTIONS_EXPORT)
    public ResponseEntity<String> exportRejections(@PathVariable(ContractEndpoints.JOB_ID) final UUID jobId) {
        final String csv = ContractImportRejectionCsv.write(
                exportContractImportJobRejections.execute(new ExportContractImportJobRejections.Query(jobId)));
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("import-" + jobId + "-rejections.csv")
                                .build()
                                .toString())
                .body(csv);
    }

    private static String usernameOf(final Jwt counselor) {
        return Optional.ofNullable(counselor.getClaimAsString(USERNAME_CLAIM))
                .orElseGet(() -> Objects.requireNonNull(counselor.getSubject()));
    }
}
