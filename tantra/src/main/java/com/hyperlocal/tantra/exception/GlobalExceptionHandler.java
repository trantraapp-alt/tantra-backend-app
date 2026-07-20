package com.hyperlocal.tantra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns validation failures thrown as IllegalArgumentException into clean 400 responses instead of
 * a generic 500, keeping a consistent error shape across the new form/listing APIs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bilingual business/validation errors → {"success":false,"message":{"en":...,"hi":...}}. */
    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<Map<String, Object>> handleLocalized(LocalizedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", ex.getLocalized());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", true);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
