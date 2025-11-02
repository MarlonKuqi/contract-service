package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.web.controller.v1.ClientController;
import com.mk.contractservice.web.controller.v1.CompanyController;
import com.mk.contractservice.web.controller.v1.PersonController;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientControllerAdvice {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClientNotFound(ClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleClientAlreadyExists(ClientAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflict",
                "message", ex.getMessage(),
                "businessKey", ex.getBusinessKey() != null ? ex.getBusinessKey() : "N/A"
        ));
    }

    @ExceptionHandler(CompanyIdentifierAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyIdentifierAlreadyExists(CompanyIdentifierAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflict",
                "message", ex.getMessage(),
                "companyIdentifier", ex.getCompanyIdentifier()
        ));
    }
}

