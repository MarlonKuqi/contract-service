package com.mk.contractservice.domain.exception;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Raised when business validation rules are violated.
 */
public class BusinessValidationException extends RuntimeException {

    private final List<Map<String, String>> violations;
    private final HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

    public BusinessValidationException(String message) {
        super(message);
        this.violations = List.of();
    }

    public BusinessValidationException(String message, List<Map<String, String>> violations) {
        super(message);
        this.violations = violations;
    }

    public List<Map<String, String>> getViolations() {
        return violations;
    }

    public HttpStatus getStatus() {
        return status;
    }
}