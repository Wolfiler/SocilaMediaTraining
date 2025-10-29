package com.socialmediatraining.authservice.dto;

import java.util.List;

public record UserSignUpRequest( //TODO Will be expanded upon when User service is implemented
         String username,
         String email,
         String password,
         List<String> roles
) {
}
