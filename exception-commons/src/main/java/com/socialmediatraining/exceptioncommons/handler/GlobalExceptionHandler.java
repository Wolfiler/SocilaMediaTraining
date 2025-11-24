package com.socialmediatraining.exceptioncommons.handler;

import com.socialmediatraining.exceptioncommons.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {//Could use aspect to log error message

    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidAuthorizationHeaderException.class)
    ResponseEntity<Map<String,String>> handleInvalidAuthorizationHeaderException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GetErrorBody(e.getMessage(),HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(UserDoesntExistsException.class)
    ResponseEntity<Map<String,String>> handleUserDoesntExistsException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GetErrorBody(e.getMessage(),HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(PostNotFoundException.class)
    ResponseEntity<Map<String,String>> handlePostNotFoundExceptionException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GetErrorBody(e.getMessage(),HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(UserActionForbiddenException.class)
    ResponseEntity<Map<String,String>> handleUserActionUnauthorizedExceptionException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GetErrorBody(e.getMessage(),HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String,String>> handleConstraintViolationException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GetErrorBody(e.getMessage(),HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {
        log.error("ResponseStatusException: {}", e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(GetErrorBody(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value())));
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    ResponseEntity<Map<String, String>> handleHttpStatusCodeException(HttpStatusCodeException e) {
        log.error("HTTP Status Code Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(GetErrorBody(
                        e.getMessage(),
                        HttpStatus.valueOf(e.getStatusCode().value())
                ));
    }

    //Mainly for proper error code in case of service instance unavailable
    @ExceptionHandler(HttpServerErrorException.class)
    ResponseEntity<Map<String, String>> handleHttpStatusCodeException(HttpServerErrorException e) {
        log.error("HTTP Status Code Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(GetErrorBody(
                        e.getMessage(),
                        HttpStatus.valueOf(e.getStatusCode().value())
                ));
    }

    private Map<String,String> GetErrorBody(String message, HttpStatus status){
        Map<String,String> error = new HashMap<>();
        error.put("HttpStatus: ", status.toString());
        error.put("Timestamp: ", Timestamp.valueOf(LocalDateTime.now()).toString());
        error.put("Message: ", message);
        return error;
    }
}
