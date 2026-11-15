package com.mk.contractservice.controllers.lamal;

import com.mk.contractservice.controllers.shared.BaseControllerAdvice;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionConflictException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotFoundException;
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

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        RecordLamalExemptionController.class,
        GetLamalExemptionController.class
})
public class LamalExemptionControllerAdvice extends BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(LamalExemptionControllerAdvice.class);
    private static final String VALIDATION_ERROR = "validationError";

    public LamalExemptionControllerAdvice(final Clock clock) {
        super(clock);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(final HttpMessageNotReadableException ex) {
        log.debug("Malformed LAMal exemption request: {}", ex.getMessage());
        return respond(problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "Validation Failed",
                "Malformed JSON or invalid date (expected yyyy-MM-dd).",
                VALIDATION_ERROR));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex) {
        log.debug("Validation failed for LAMal exemption request: {}", ex.getMessage());
        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "Validation Failed",
                "One or more fields are invalid or missing.",
                VALIDATION_ERROR);
        final List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of("field", fe.getField(), "message", messageOf(fe)))
                .toList();
        problemDetail.setProperty("validations", errors);
        return respond(problemDetail);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(final DomainValidationException ex) {
        log.debug("LAMal exemption refused: {}", ex.getMessage());
        return respond(problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "Domain Validation Error",
                ex.getMessage(),
                ex.code() != null ? ex.code() : "domainValidationError"));
    }

    @ExceptionHandler(LamalExemptionConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(final LamalExemptionConflictException ex) {
        log.debug("LAMal exemption refused: {}", ex.getMessage());
        return respond(problem(HttpStatus.CONFLICT,
                "LAMal Exemption Conflict",
                ex.getMessage(),
                "lamalExemptionConflict"));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFound(final ClientNotFoundException ex) {
        log.debug("Client not found: {}", ex.getMessage());
        return respond(problem(HttpStatus.NOT_FOUND, "Client Not Found", ex.getMessage(), "clientNotFound"));
    }

    @ExceptionHandler(LamalExemptionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleExemptionNotFound(final LamalExemptionNotFoundException ex) {
        log.debug("LAMal exemption not found: {}", ex.getMessage());
        return respond(problem(HttpStatus.NOT_FOUND,
                "LAMal Exemption Not Found",
                ex.getMessage(),
                "lamalExemptionNotFound"));
    }
}
