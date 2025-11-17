package com.socialmediatraining.authservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

public record UserSignUpRequest(
         @NotBlank
         @Length(min = 3, max = 25, message = "Username must be between 3 and 25 characters")
         @Pattern(regexp = "^[a-z_]*$", message = "Forbidden characters in username. It must only contain lower case letters and underscores")
         String username,
         @Email
         String email,
         @NotBlank
         String password,
         @Size(min = 1, message = "User must have at least one role (should at least be USER)")
         @UniqueElements(message = "Roles must not contain duplicates")
         List<@Pattern(regexp = "USER|ADMIN", message = "Invalid role. Must be either USER or ADMIN") String> roles,
         String firstName,
         String lastName,
         @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
         String dateOfBirth,
         String description
) {

    public UserSignUpRequest {
        if (roles != null) {
            roles = roles.stream().map(String::toUpperCase).toList();
        }
    }
}
