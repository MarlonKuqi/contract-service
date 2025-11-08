package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.exception.DomainValidationException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed",
                "One or more fields are invalid or missing.", "validationError");

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", messageOf(fe),
                        "rejectedValue", Optional.ofNullable(fe.getRejectedValue()).orElse("null")))
                .toList();

        problemDetail.setProperty("validations", errors);
        return respond(problemDetail);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex) {
        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Domain Validation Error",
                ex.getMessage(), ex.getCode() != null ? ex.getCode() : "domainValidationError");
        return respond(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), "badRequest");
        return respond(problemDetail);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleClientAlreadyExists(ClientAlreadyExistsException ex) {
        log.debug("Client already exists: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Client Already Exists",
                ex.getMessage(), "clientAlreadyExists");
        if (ex.getBusinessKey() != null) {
            problemDetail.setProperty("businessKey", ex.getBusinessKey());
        }
        return respond(problemDetail);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFound(ClientNotFoundException ex) {
        log.debug("Client not found: {}", ex.getMessage());

        ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
        return respond(problemDetail);
    }

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

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Contract Expired",
                ex.getMessage(), "contractExpired");
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

    private static String messageOf(FieldError fe) {
        return Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value");
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

