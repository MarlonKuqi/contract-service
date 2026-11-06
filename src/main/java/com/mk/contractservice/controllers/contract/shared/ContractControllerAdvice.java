package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.controllers.contract.ContractImportJobController;
import com.mk.contractservice.controllers.contract.CreateContractController;
import com.mk.contractservice.controllers.contract.ExportContractController;
import com.mk.contractservice.controllers.contract.ImportContractsController;
import com.mk.contractservice.controllers.contract.PatchContractCostController;
import com.mk.contractservice.controllers.contract.SearchContractController;
import com.mk.contractservice.controllers.shared.BaseControllerAdvice;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobNotFoundException;
import com.mk.contractservice.domain.contract.importjob.exception.IdempotencyKeyReusedException;
import com.mk.contractservice.domain.contract.importjob.exception.InvalidIdempotencyKeyException;
import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.importjob.exception.InvalidImportFileException;
import com.mk.contractservice.domain.contract.exception.InvalidImportSizeException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.domain.shared.exception.DomainValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice(assignableTypes = {
        ContractImportJobController.class,
        CreateContractController.class,
        ExportContractController.class,
        ImportContractsController.class,
        PatchContractCostController.class,
        SearchContractController.class
})
public class ContractControllerAdvice extends BaseControllerAdvice {

    public static final String CONTEXT = "context";
    private static final Logger log = LoggerFactory.getLogger(ContractControllerAdvice.class);

    public ContractControllerAdvice(final Clock clock) {
        super(clock);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFoundForContract(ClientNotFoundException ex) {
        log.debug("Client not found for contract: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
        problemDetail.setProperty(CONTEXT, "Cannot create contract for non-existent client");
        return respond(problemDetail);
    }

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleContractNotFound(ContractNotFoundException ex) {
        log.debug("Contract not found: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Contract Not Found",
                ex.getMessage(), "contractNotFound");
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidExportCursorException.class)
    public ResponseEntity<ProblemDetail> handleInvalidExportCursor(InvalidExportCursorException ex) {
        log.debug("Invalid export cursor");

        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid Cursor",
                ex.getMessage(), "invalidCursor");
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidImportSizeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidImportSize(InvalidImportSizeException ex) {
        log.debug("Invalid import size: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid Import Size",
                ex.getMessage(), "invalidImportSize");
        return respond(problemDetail);
    }

    @ExceptionHandler(ContractImportRejectedException.class)
    public ResponseEntity<ProblemDetail> handleContractImportRejected(ContractImportRejectedException ex) {
        log.debug("Contract import rejected: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Contract Import Rejected",
                "No contract was created. Fix the rejected lines and send the whole import again.",
                "contractImportRejected");
        problemDetail.setProperty("rejections", ex.rejections().stream()
                .map(rejection -> Map.of(
                        "index", rejection.index(),
                        "clientId", rejection.clientId(),
                        "code", rejection.reason().code(),
                        "message", rejection.message()))
                .toList());
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidImportFileException.class)
    public ResponseEntity<ProblemDetail> handleInvalidImportFile(InvalidImportFileException ex) {
        log.debug("Invalid import file: {}", ex.getMessage());

        return respond(problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid Import File",
                ex.getMessage(), "invalidImportFile"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleImportFileTooLarge(MaxUploadSizeExceededException ex) {
        log.debug("Import file too large: {}", ex.getMessage());

        return respond(problem(HttpStatus.CONTENT_TOO_LARGE, "Import File Too Large",
                "The file exceeds the maximum allowed size.", "importFileTooLarge"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingFilePart(MissingServletRequestPartException ex) {
        log.debug("Missing request part: {}", ex.getRequestPartName());

        return respond(problem(HttpStatus.BAD_REQUEST, "Missing Required Parameter",
                "Required part '" + ex.getRequestPartName() + "' is missing", "missingParameter"));
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ProblemDetail> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        log.debug("Invalid idempotency key: {}", ex.getMessage());

        return respond(problem(HttpStatus.BAD_REQUEST, "Invalid Idempotency Key",
                ex.getMessage(), "invalidIdempotencyKey"));
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyKeyReused(IdempotencyKeyReusedException ex) {
        log.debug("Idempotency key reused: {}", ex.getMessage());

        return respond(problem(HttpStatus.UNPROCESSABLE_CONTENT, "Idempotency Key Reused",
                ex.getMessage(), "idempotencyKeyReused"));
    }

    @ExceptionHandler(ContractImportJobNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleContractImportJobNotFound(ContractImportJobNotFoundException ex) {
        log.debug("Contract import job not found: {}", ex.getMessage());

        return respond(problem(HttpStatus.NOT_FOUND, "Contract Import Job Not Found",
                ex.getMessage(), "contractImportJobNotFound"));
    }

    @ExceptionHandler(ExpiredContractException.class)
    public ResponseEntity<ProblemDetail> handleExpiredContract(ExpiredContractException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Expired Contract",
                ex.getMessage(), "contractExpired");
        return respond(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed JSON in contract request: {}", ex.getMessage());

        Throwable cause = ex.getCause();
        String detail = switch (cause) {
            case InvalidFormatException ife -> String.format(
                    "Invalid value '%s' for field '%s'. Expected type: %s",
                    ife.getValue(),
                    fieldPathOf(ife),
                    ife.getTargetType().getSimpleName()
            );
            case MismatchedInputException mie when !mie.getPath().isEmpty() -> String.format(
                    "Missing or invalid field: '%s'",
                    fieldPathOf(mie)
            );
            case null, default -> "Malformed JSON or invalid payload.";
        };

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed", detail, "validationError");
        return respond(problemDetail);
    }

    private static String fieldPathOf(MismatchedInputException ex) {
        StringBuilder path = new StringBuilder();
        ex.getPath().forEach(reference -> {
            if (reference.getPropertyName() == null) {
                path.append('[').append(reference.getIndex()).append(']');
            } else {
                path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
            }
        });
        return path.toString();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        log.debug("Validation failed for contract request: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed",
                "One or more fields are invalid or missing.", "validationError");

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", messageOf(fe),
                        "rejectedValue", Optional.ofNullable(fe.getRejectedValue()).orElse("null")))
                .toList();

        problemDetail.setProperty("validations", errors);
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidContractCostException.class)
    public ResponseEntity<ProblemDetail> handleInvalidContractCost(InvalidContractCostException ex) {
        log.warn("Invalid contract cost: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid Contract Cost",
                ex.getMessage(), "invalidContractCost");
        problemDetail.setProperty(CONTEXT, "Contract cost validation failed");
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidContractPeriodException.class)
    public ResponseEntity<ProblemDetail> handleInvalidContractPeriod(InvalidContractPeriodException ex) {
        log.warn("Invalid contract period: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid Contract Period",
                ex.getMessage(), "invalidContractPeriod");
        problemDetail.setProperty(CONTEXT, "Contract period validation failed");
        return respond(problemDetail);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex) {
        log.warn("Domain validation failed for contract: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Domain Validation Failed",
                ex.getMessage(), ex.code() != null ? ex.code() : "domainValidationError");
        problemDetail.setProperty(CONTEXT, "Contract domain validation failed");
        return respond(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleContractValidationError(IllegalArgumentException ex) {
        log.warn("Contract validation failed: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Contract Validation Failed",
                ex.getMessage(), "contractValidationError");
        problemDetail.setProperty(CONTEXT, "Contract validation failed");
        return respond(problemDetail);
    }
}

