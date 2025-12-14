package com.mk.contractservice.web;

import com.mk.contractservice.infrastructure.shared.exception.InvalidPaginationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalControllerAdvice extends BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);


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

        String errorMessage = ex.getMessage();

        if (errorMessage == null) {
            return respond(problem(HttpStatus.CONFLICT, "Constraint Violation",
                    "A data constraint violation occurred.", "dataIntegrityViolation"));
        }

        ConstraintType constraintType = detectConstraintType(errorMessage);
        final String message = switch (constraintType) {
            case EMAIL -> "A client with this email address already exists.";
            case COMPANY_IDENTIFIER -> "A company with this identifier already exists.";
            case UNKNOWN -> "A data constraint violation occurred.";
        };

        final String code = switch (constraintType) {
            case EMAIL -> "emailAlreadyExists";
            case COMPANY_IDENTIFIER -> "companyIdentifierAlreadyExists";
            case UNKNOWN -> "dataIntegrityViolation";
        };

        return respond(problem(HttpStatus.CONFLICT, "Constraint Violation", message, code));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.debug("Validation failed: {}", ex.getMessage());

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = problem(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                errorMessage,
                "validationFailed"
        );

        List<String> fieldErrorDetails = fieldErrors.stream()
                .map(error -> String.format("Field '%s' %s (rejected value: %s)",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .toList();
        problemDetail.setProperty("errors", fieldErrorDetails);

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


    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("prod");
    }
}


