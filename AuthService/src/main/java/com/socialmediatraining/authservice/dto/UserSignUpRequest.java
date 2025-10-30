package com.socialmediatraining.authservice.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;
import java.util.stream.Collectors;

public record UserSignUpRequest( //TODO Will be expanded upon when User service is implemented
         @NotBlank
         String username,
         @Email
         String email,
         @NotBlank
         String password,
         @Size(min = 1, message = "User must have at least one role (should at least be USER)")
         @UniqueElements(message = "Roles must not contain duplicates")
         List<@Pattern(regexp = "USER|ADMIN", message = "Invalid role. Must be either USER or ADMIN")
                 String> roles
) {

    public UserSignUpRequest {
        if (roles != null) {
            roles = roles.stream().map(String::toUpperCase).toList();
        }
    }
}
