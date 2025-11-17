package com.socialmediatraining.exceptioncommons.exception;

public class UserDoesntExistsException extends RuntimeException{
    public UserDoesntExistsException(String message) {
        super(message);
    }
}
