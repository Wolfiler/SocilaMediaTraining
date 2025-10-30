package com.socialmediatraining.exceptioncommons.handler;

import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import com.socialmediatraining.exceptioncommons.exception.InvalidAuthorizationHeaderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAuthorizationHeaderException.class)
    ResponseEntity<Map<String,String>> handleInvalidAuthorizationHeaderException(RuntimeException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GetErrorBody(e.getMessage(),HttpStatus.FORBIDDEN));
    }

    //I don't really like this error, the status could be wrong most of the time. Might need to change.
    @ExceptionHandler(AuthUserCreationException.class)
    ResponseEntity<Map<String,String>> handleAuthUserCreationException(RuntimeException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GetErrorBody(e.getMessage(),HttpStatus.BAD_REQUEST));
    }

    private Map<String,String> GetErrorBody(String message, HttpStatus status){
        Map<String,String> error = new HashMap<>();
        error.put("HttpStatus: ", status.toString());
        error.put("Timestamp: ", Timestamp.valueOf(LocalDateTime.now()).toString());
        error.put("Message: ", message);
        return error;
    }
}
