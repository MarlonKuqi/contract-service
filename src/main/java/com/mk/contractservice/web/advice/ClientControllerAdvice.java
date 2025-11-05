package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.web.controller.v1.ClientController;
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

@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ClientControllerAdvice.class);

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleClientNotFound(ClientNotFoundException ex) {
        log.debug("Client not found: {}", ex.getMessage());
        ProblemDetail problemDetail = problem(HttpStatus.NOT_FOUND, "Client Not Found",
                ex.getMessage(), "clientNotFound");
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

