package com.mk.contractservice.web.advice;

import com.mk.contractservice.web.controller.v1.CompanyController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CompanyController.class)
public class CompanyControllerAdvice {
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handlePersonSpecific(IllegalStateException e) {
        return ResponseEntity.badRequest().body("Company error: " + e.getMessage());
    }
}
