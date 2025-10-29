package com.example.tasktracker.controller;

import com.example.tasktracker.service.TaskServiceException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(TaskServiceException.class)
  public ResponseEntity<Map<String, Object>> handleTaskNotFound(TaskServiceException ex) {
    Map<String, Object> body = Map.of(
        "timestamp", Instant.now().toString(),
        "message", ex.getMessage(),
        "status", HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
        .findFirst()
        .orElse("Validation failed");
    Map<String, Object> body = Map.of(
        "timestamp", Instant.now().toString(),
        "message", message,
        "status", HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
