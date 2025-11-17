package com.socialmediatraining.authservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record UserUpdateRequest(
        @NotBlank
        @Length(min = 3, max = 25, message = "Username must be between 3 and 25 characters")
        @Pattern(regexp = "^[a-z_]*$", message = "Forbidden characters in username. It must only contain lower case letters and underscores")
        String username,
        @Email
        String email,
        String firstName,
        String lastName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        String dateOfBirth,
        String description,
        String profilePicture
) {
}
