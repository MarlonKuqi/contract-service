package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.web.controller.v1.ContractController;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {ContractController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContractControllerAdvice {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClientNotFoundForContract(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage(),
                "context", "Cannot create contract for non-existent client"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleContractValidationError(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "context", "Contract validation failed"
        ));
    }
}

