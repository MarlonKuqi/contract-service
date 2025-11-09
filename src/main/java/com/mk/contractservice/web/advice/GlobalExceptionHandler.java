package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

        ProblemDetail problemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Contract Expired",
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

