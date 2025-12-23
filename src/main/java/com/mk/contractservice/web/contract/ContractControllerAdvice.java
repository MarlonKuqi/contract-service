package com.mk.contractservice.web.contract;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.DomainValidationException;
import com.mk.contractservice.web.BaseControllerAdvice;
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

@RestControllerAdvice(assignableTypes = {ContractController.class})
public class ContractControllerAdvice extends BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ContractControllerAdvice.class);

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFoundForContract(ClientNotFoundException ex) {
        log.debug("Client not found for contract: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
        pd.setProperty("context", "Cannot create contract for non-existent client");
        return respond(pd);
    }

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleContractNotFound(ContractNotFoundException ex) {
        log.debug("Contract not found: {}", ex.getMessage());

        ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "Contract Not Found",
                ex.getMessage(), "contractNotFound");
        return respond(pd);
    }

    @ExceptionHandler(ContractNotOwnedByClientException.class)
    public ResponseEntity<ProblemDetail> handleContractNotOwnedByClient(ContractNotOwnedByClientException ex) {
        log.warn("Security: Attempt to access contract not owned by client: {}", ex.getMessage());

        ProblemDetail pd = problem(HttpStatus.FORBIDDEN, "Access Denied",
                "You do not have permission to access this contract", "contractAccessDenied");
        return respond(pd);
    }

    @ExceptionHandler(ExpiredContractException.class)
    public ResponseEntity<ProblemDetail> handleExpiredContract(ExpiredContractException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Expired Contract",
                ex.getMessage(), "contractExpired");
        return respond(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed JSON in contract request: {}", ex.getMessage());

        Throwable cause = ex.getCause();
        String detail = switch (cause) {
            case InvalidFormatException ife -> String.format(
                    "Invalid value '%s' for field '%s'. Expected type: %s",
                    ife.getValue(),
                    ife.getPath().get(0).getFieldName(),
                    ife.getTargetType().getSimpleName()
            );
            case MismatchedInputException mie when !mie.getPath().isEmpty() -> String.format(
                    "Missing or invalid field: '%s'",
                    mie.getPath().get(0).getFieldName()
            );
            default -> "Malformed JSON or invalid payload.";
        };

        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed", detail, "validationError");
        return respond(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        log.debug("Validation failed for contract request: {}", ex.getMessage());

        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed",
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
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid Contract Cost",
                ex.getMessage(), "invalidContractCost");
        pd.setProperty("context", "Contract cost validation failed");
        return respond(pd);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex) {
        log.warn("Domain validation failed for contract: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Domain Validation Failed",
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
}

