package com.mk.contractservice.web.advice;

import com.mk.contractservice.web.controller.v1.PersonController;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice(assignableTypes = PersonController.class)
public class PersonControllerAdvice {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(final HttpMessageNotReadableException ex) {
        final var dataProblem = problem(HttpStatus.BAD_REQUEST, "Bad Request",
                "Malformed JSON or invalid payload.", "badRequest");
        return respond(dataProblem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handlePersonSpecific(final IllegalStateException e) {
        return ResponseEntity.badRequest().body("Person error: " + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(MethodArgumentNotValidException ex) {
        var pd = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Some fields are invalid or missing.", "validationError");
        // Ajouter les erreurs de champs
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.<String, Object>of(
                        "field", fe.getField(),
                        "message", messageOf(fe),
                        "rejectedValue", fe.getRejectedValue()))
                .toList();
        pd.setProperty("errors", errors);
        return respond(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        var pd = problem(HttpStatus.BAD_REQUEST, "Constraint Violation",
                "One or more parameters are invalid.", "constraintViolation");
        pd.setProperty("errors", ex.getConstraintViolations().stream()
                .map(cv -> Map.of("param", cv.getPropertyPath().toString(), "message", cv.getMessage()))
                .toList());
        return respond(pd);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class) // crée ta custom exception si pas déjà faite
    public ResponseEntity<ProblemDetail> handleConflict(ClientAlreadyExistsException ex) {
        var pd = problem(HttpStatus.CONFLICT, "Conflict",
                "Client already exists.", "clientAlreadyExists");
        return respond(pd);
    }

    // ====== 422: Règles métier non respectées ======
    @ExceptionHandler(BusinessValidationException.class) // crée ta custom exception
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessValidationException ex) {
        var pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business Validation Failed",
                ex.getMessage() != null ? ex.getMessage() : "Cannot create client due to business rules.",
                "businessValidationFailed");
        // Optionnel: détailler les violations métier
        if (ex.getViolations() != null) {
            pd.setProperty("errors", ex.getViolations());
        }
        return respond(pd);
    }

    // ====== 500: fallback ======
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(Exception ex, WebRequest request) {
        // Log interne ici (logger.error(..., ex))
        var pd = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Unexpected error.", "internalError"); // même code pour 5xx
        return respond(pd);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("code", code);
        pd.setProperty("timestamp", OffsetDateTime.now());
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

    private static String messageOf(FieldError fe) {
        return Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value");
    }
}
