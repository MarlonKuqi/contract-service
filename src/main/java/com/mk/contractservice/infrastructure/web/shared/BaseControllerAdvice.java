package com.mk.contractservice.infrastructure.web.shared;

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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public abstract class BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(BaseControllerAdvice.class);

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
        log.warn("Data integrity violation (this should be rare - check business validations): {}", ex.getMessage());

        // Return a generic message to avoid leaking database schema details
        return respond(problem(
                HttpStatus.CONFLICT,
                "Constraint Violation",
                "The operation could not be completed due to a data constraint violation. Please verify your data and try again.",
                "dataIntegrityViolation"
        ));
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

    protected ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
        return problemDetail;
    }

    protected ProblemDetail problemWithStackTrace(HttpStatus status, String title, String detail, String code, Exception ex) {
        ProblemDetail problemDetail = problem(status, title, detail, code);
        problemDetail.setProperty("exception", ex.getClass().getSimpleName());
        problemDetail.setProperty("stackTrace", getStackTrace(ex));
        return problemDetail;
    }

    protected ResponseEntity<ProblemDetail> respond(ProblemDetail problemDetail) {
        String lang = LocaleContextHolder.getLocale().toLanguageTag();
        return ResponseEntity
                .status(problemDetail.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LANGUAGE, lang)
                .body(problemDetail);
    }

    protected String messageOf(FieldError fieldError) {
        return Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value");
    }

    protected String getStackTrace(Exception ex) {
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
