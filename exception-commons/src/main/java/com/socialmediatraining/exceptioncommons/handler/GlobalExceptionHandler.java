package com.socialmediatraining.exceptioncommons.handler;

import com.socialmediatraining.exceptioncommons.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {//TODO could use aspect to log error message

    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidAuthorizationHeaderException.class)
    ResponseEntity<Map<String,String>> handleInvalidAuthorizationHeaderException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GetErrorBody(e.getMessage(),HttpStatus.FORBIDDEN));
    }

    //I don't really like this error, the status could be wrong most of the time. Might need to change.
    @ExceptionHandler(AuthUserCreationException.class)
    ResponseEntity<Map<String,String>> handleAuthUserCreationException(RuntimeException e){
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GetErrorBody(e.getMessage(),HttpStatus.BAD_REQUEST));
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

    private Map<String,String> GetErrorBody(String message, HttpStatus status){
        Map<String,String> error = new HashMap<>();
        error.put("HttpStatus: ", status.toString());
        error.put("Timestamp: ", Timestamp.valueOf(LocalDateTime.now()).toString());
        error.put("Message: ", message);
        return error;
    }
}
