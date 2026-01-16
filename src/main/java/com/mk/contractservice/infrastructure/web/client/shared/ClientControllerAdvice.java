package com.mk.contractservice.infrastructure.web.client.shared;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.PhoneAlreadyExistsException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.domain.shared.exception.DomainValidationException;
import com.mk.contractservice.infrastructure.web.client.CreateClientController;
import com.mk.contractservice.infrastructure.web.client.DeleteClientController;
import com.mk.contractservice.infrastructure.web.client.PatchClientController;
import com.mk.contractservice.infrastructure.web.client.SearchClientController;
import com.mk.contractservice.infrastructure.web.client.UpdateClientController;
import com.mk.contractservice.infrastructure.web.shared.BaseControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice(assignableTypes = {
        CreateClientController.class,
        UpdateClientController.class,
        DeleteClientController.class,
        PatchClientController.class,
        SearchClientController.class
})
public class ClientControllerAdvice extends BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ClientControllerAdvice.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(final HttpMessageNotReadableException ex) {
        log.debug("Malformed JSON in client request: {}", ex.getMessage());

        final Throwable cause = ex.getCause();
        final String detail = switch (cause) {
            case InvalidFormatException ife -> String.format(
                    "Invalid value '%s' for field '%s'. Expected type: %s",
                    ife.getValue(),
                    ife.getPath().getFirst().getFieldName(),
                    ife.getTargetType().getSimpleName()
            );
            case MismatchedInputException mie when !mie.getPath().isEmpty() -> String.format(
                    "Missing or invalid field: '%s'",
                    mie.getPath().getFirst().getFieldName()
            );
            case null, default -> "Malformed JSON or invalid payload.";
        };

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed", detail, "validationError");
        return respond(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex) {
        log.debug("Validation failed for client request: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed",
                "One or more fields are invalid or missing.", "validationError");

        final List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
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

        final ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Domain Validation Error",
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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyExists(final EmailAlreadyExistsException ex) {
        log.debug("Email already exists: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Email Already Exists",
                ex.getMessage(), "emailAlreadyExists");
        problemDetail.setProperty("email", ex.getEmail());
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

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handlePhoneAlreadyExists(final PhoneAlreadyExistsException ex) {
        log.debug("Phone number already exists: {}", ex.getMessage());

        final ProblemDetail problemDetail = problem(HttpStatus.CONFLICT, "Phone Number Already Exists",
                ex.getMessage(), "phoneAlreadyExists");
        problemDetail.setProperty("phoneNumber", ex.getPhoneNumber());
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
}

