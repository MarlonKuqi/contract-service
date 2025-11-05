package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.web.controller.v1.CompanyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = CompanyController.class)
public class CompanyControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(CompanyControllerAdvice.class);

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleClientAlreadyExists(ClientAlreadyExistsException ex) {
        log.debug("Client already exists for company: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Email Already Exists",
                ex.getMessage(), "emailAlreadyExists");
        if (ex.getBusinessKey() != null) {
            pd.setProperty("email", ex.getBusinessKey());
        }
        return respond(pd);
    }

    @ExceptionHandler(CompanyIdentifierAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleCompanyIdentifierAlreadyExists(CompanyIdentifierAlreadyExistsException ex) {
        log.debug("Company identifier already exists: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Company Identifier Already Exists",
                ex.getMessage(), "companyIdentifierAlreadyExists");
        pd.setProperty("companyIdentifier", ex.getCompanyIdentifier());
        return respond(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleCompanySpecificError(IllegalStateException ex) {
        log.warn("Company-specific validation failed: {}", ex.getMessage());
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Company Error",
                ex.getMessage(), "companyError");
        pd.setProperty("context", "Company-specific validation failed");
        return respond(pd);
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
