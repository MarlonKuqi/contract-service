package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import com.mk.contractservice.infrastructure.exception.InvalidPaginationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleContractNotFound(ContractNotFoundException ex) {
        log.debug("Contract not found: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Contract Not Found",
                ex.getMessage(), "contractNotFound");
        return respond(problemDetail);
    }

    @ExceptionHandler(ContractNotOwnedByClientException.class)
    public ResponseEntity<ProblemDetail> handleContractNotOwnedByClient(ContractNotOwnedByClientException ex) {
        log.warn("Security: Attempt to access contract not owned by client: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.FORBIDDEN, "Access Denied",
                "You do not have permission to access this contract", "contractAccessDenied");
        return respond(problemDetail);
    }

    @ExceptionHandler(ExpiredContractException.class)
    public ResponseEntity<ProblemDetail> handleExpiredContract(ExpiredContractException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Expired Contract",
                ex.getMessage(), "contractExpired");
        return respond(problemDetail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        log.debug("Missing required request parameter: {}", ex.getParameterName());

        String message = String.format("Required request parameter '%s' is missing", ex.getParameterName());
        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Missing Required Parameter",
                message, "missingParameter");
        problemDetail.setProperty("parameterName", ex.getParameterName());
        problemDetail.setProperty("parameterType", ex.getParameterType());
        return respond(problemDetail);
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPagination(InvalidPaginationException ex) {
        log.debug("Invalid pagination parameter: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid Parameter",
                ex.getMessage(), "invalidParameter");
        return respond(problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: The resource was modified by another user. Entity: {}, ID: {}",
                ex.getPersistentClassName(), ex.getIdentifier());

        ProblemDetail problemDetail = problem(
                HttpStatus.CONFLICT,
                "Concurrent Modification Detected",
                "The resource you are trying to modify was updated by another user. Please refresh the data and try again.",
                "optimisticLockFailure"
        );
        problemDetail.setProperty("entityType", ex.getPersistentClassName());
        problemDetail.setProperty("entityId", ex.getIdentifier());
        return respond(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        String message = "A data constraint violation occurred.";
        String code = "dataIntegrityViolation";

        if (ex.getMessage() != null && ex.getMessage().contains("email")) {
            message = "A client with this email address already exists.";
            code = "emailAlreadyExists";
        } else if (ex.getMessage() != null && ex.getMessage().contains("company_identifier")) {
            message = "A company with this identifier already exists.";
            code = "companyIdentifierAlreadyExists";
        }

        ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Constraint Violation", message, code);
        return respond(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = isProductionEnvironment()
                ? problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.", "internalError")
                : problemWithStackTrace(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ex.getMessage(), "internalError", ex);

        return respond(problemDetail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
        return problemDetail;
    }

    private static ProblemDetail problemWithStackTrace(HttpStatus status, String title, String detail,
                                                       String code, Exception ex) {
        ProblemDetail problemDetail = problem(status, title, detail, code);
        problemDetail.setProperty("exception", ex.getClass().getSimpleName());
        problemDetail.setProperty("stackTrace", getStackTrace(ex));
        return problemDetail;
    }

    private static ResponseEntity<ProblemDetail> respond(final ProblemDetail problemDetail) {
        final String lang = LocaleContextHolder.getLocale().toLanguageTag();
        return ResponseEntity
                .status(problemDetail.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LANGUAGE, lang)
                .body(problemDetail);
    }

    private static String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        return sb.toString();
    }

    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("prod");
    }
}

