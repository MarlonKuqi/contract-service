package com.mk.contractservice.controllers.shared;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;

@RequiredArgsConstructor
@RestControllerAdvice
public class SecurityControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    private final Clock clock;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        final ProblemDetail problemDetail = ProblemDetails.problem(clock, HttpStatus.UNAUTHORIZED, "Unauthorized",
                "A valid bearer token is required.", "unauthenticated");
        return ProblemDetails.responseFor(problemDetail)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        final ProblemDetail problemDetail = ProblemDetails.problem(clock, HttpStatus.FORBIDDEN, "Forbidden",
                "You are not allowed to perform this operation.", "accessDenied");
        return ProblemDetails.responseFor(problemDetail).body(problemDetail);
    }
}
