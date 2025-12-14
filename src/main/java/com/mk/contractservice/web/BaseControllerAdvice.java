package com.mk.contractservice.web;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


public abstract class BaseControllerAdvice {

    protected ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("traceId", UUID.randomUUID().toString());
        return problemDetail;
    }

    protected ProblemDetail problemWithStackTrace(HttpStatus status, String title, String detail,
                                                  String code, Exception ex) {
        ProblemDetail problemDetail = problem(status, title, detail, code);
        problemDetail.setProperty("exception", ex.getClass().getSimpleName());
        problemDetail.setProperty("stackTrace", getStackTrace(ex));
        return problemDetail;
    }

    protected ResponseEntity<ProblemDetail> respond(ProblemDetail problemDetail) {
        String lang = LocaleContextHolder.getLocale().toLanguageTag();
        return ResponseEntity
                .status(problemDetail.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LANGUAGE, lang)
                .body(problemDetail);
    }

    protected String messageOf(FieldError fieldError) {
        return Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value");
    }

    protected String getStackTrace(Exception ex) {
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

    protected enum ConstraintType {
        EMAIL,
        COMPANY_IDENTIFIER,
        UNKNOWN
    }

    protected ConstraintType detectConstraintType(String errorMessage) {
        if (errorMessage.contains("email")) {
            return ConstraintType.EMAIL;
        }
        if (errorMessage.contains("company_identifier")) {
            return ConstraintType.COMPANY_IDENTIFIER;
        }
        return ConstraintType.UNKNOWN;
    }
}

