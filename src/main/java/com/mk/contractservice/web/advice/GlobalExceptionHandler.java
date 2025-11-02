package com.mk.contractservice.web.advice;

import com.mk.contractservice.domain.exception.DomainValidationException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, Object>> handleDomainValidation(DomainValidationException ex) {
        var body = new HashMap<String, Object>();
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        if (ex.getCode() != null) {
            body.put("code", ex.getCode());
        }
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }
}

