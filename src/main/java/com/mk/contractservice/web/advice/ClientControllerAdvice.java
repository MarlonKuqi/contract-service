package com.mk.contractservice.web.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.exception.DomainValidationException;
import com.mk.contractservice.web.controller.v1.ClientController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ClientControllerAdvice.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(final HttpMessageNotReadableException ex) {
        log.debug("Malformed JSON in client request: {}", ex.getMessage());

        String detail = "Malformed JSON or invalid payload.";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            detail = String.format("Invalid value '%s' for field '%s'. Expected type: %s",
                    ife.getValue(), ife.getPath().get(0).getFieldName(), ife.getTargetType().getSimpleName());
        } else if (cause instanceof MismatchedInputException mie) {
            if (!mie.getPath().isEmpty()) {
                detail = String.format("Missing or invalid field: '%s'", mie.getPath().get(0).getFieldName());
            }
        }

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed", detail, "validationError");
        return respond(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex) {
        log.debug("Validation failed for client request: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed",
                "One or more fields are invalid or missing.", "validationError");

        final List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", messageOf(fe),
                        "rejectedValue", Optional.ofNullable(fe.getRejectedValue()).orElse("null")))
                .toList();

        problemDetail.setProperty("validations", errors);
        return respond(problemDetail);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(final DomainValidationException ex) {
        log.debug("Domain validation error for client: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Domain Validation Error",
                ex.getMessage(), ex.getCode() != null ? ex.getCode() : "domainValidationError");
        return respond(problemDetail);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleClientAlreadyExists(final ClientAlreadyExistsException ex) {
        log.debug("Client already exists: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Client Already Exists",
                ex.getMessage(), "clientAlreadyExists");
        if (ex.getBusinessKey() != null) {
            problemDetail.setProperty("businessKey", ex.getBusinessKey());
        }
        return respond(problemDetail);
    }

    @ExceptionHandler(CompanyIdentifierAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleCompanyIdentifierAlreadyExists(final CompanyIdentifierAlreadyExistsException ex) {
        log.debug("Company identifier already exists: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Company Identifier Already Exists",
                ex.getMessage(), "companyIdentifierAlreadyExists");
        problemDetail.setProperty("companyIdentifier", ex.getCompanyIdentifier());
        return respond(problemDetail);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFound(final ClientNotFoundException ex) {
        log.debug("Client not found: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
        return respond(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(final IllegalArgumentException ex) {
        log.warn("Illegal argument in client operation: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), "badRequest");
        return respond(problemDetail);
    }

    private static String messageOf(final FieldError fe) {
        return Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value");
    }

    private static ProblemDetail problem(final HttpStatus status, final String title, final String detail, final String code) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
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
}

