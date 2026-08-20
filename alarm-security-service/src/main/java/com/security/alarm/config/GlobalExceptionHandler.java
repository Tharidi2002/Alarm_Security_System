package com.security.alarm.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("status", 403);
        response.put("message", e.getMessage());
        response.put("type", "SECURITY_ERROR");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}