package com.socialmediatraining.exceptioncommons.exception;

public class UserActionForbiddenException extends RuntimeException {
    public UserActionForbiddenException(String message) {
        super(message);
    }
}
