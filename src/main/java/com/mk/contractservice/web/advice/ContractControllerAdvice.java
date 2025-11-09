package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.DomainValidationException;
import com.mk.contractservice.domain.exception.InvalidContractCostException;
import com.mk.contractservice.web.controller.v1.ContractController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

@RestControllerAdvice(assignableTypes = {ContractController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContractControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ContractControllerAdvice.class);

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFoundForContract(ClientNotFoundException ex) {
        log.debug("Client not found for contract: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
        pd.setProperty("context", "Cannot create contract for non-existent client");
        return respond(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        log.debug("Validation failed for contract request: {}", ex.getMessage());

        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed",
                "One or more fields are invalid or missing.", "validationError");

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", messageOf(fe),
                        "rejectedValue", Optional.ofNullable(fe.getRejectedValue()).orElse("null")))
                .toList();

        pd.setProperty("validations", errors);
        return respond(pd);
    }

    @ExceptionHandler(InvalidContractCostException.class)
    public ResponseEntity<ProblemDetail> handleInvalidContractCost(InvalidContractCostException ex) {
        log.warn("Invalid contract cost: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Contract Cost",
                ex.getMessage(), "invalidContractCost");
        pd.setProperty("context", "Contract cost validation failed");
        return respond(pd);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex) {
        log.warn("Domain validation failed for contract: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Domain Validation Failed",
                ex.getMessage(), ex.getCode() != null ? ex.getCode() : "domainValidationError");
        pd.setProperty("context", "Contract domain validation failed");
        return respond(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleContractValidationError(IllegalArgumentException ex) {
        log.warn("Contract validation failed: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Contract Validation Failed",
                ex.getMessage(), "contractValidationError");
        pd.setProperty("context", "Contract validation failed");
        return respond(pd);
    }

    private static String messageOf(FieldError fe) {
        return Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("code", code);
        pd.setProperty("timestamp", LocalDateTime.now());
        pd.setProperty("traceId", UUID.randomUUID().toString());
        return pd;
    }

    private static ResponseEntity<ProblemDetail> respond(final ProblemDetail problemDetail) {
        final String lang = LocaleContextHolder.getLocale().toLanguageTag();
        return ResponseEntity
                .status(problemDetail.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LANGUAGE, lang)
                .body(problemDetail);
    }
}

