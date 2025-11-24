package com.socialmediatraining.authservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;

public record UserUpdateRequest(
        @Email
        String email,
        String firstName,
        String lastName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        String dateOfBirth,
        String description,
        String profilePicture
) {
        public static UserUpdateRequest create(String email, String firstName, String lastName, String dateOfBirth,
                                               String description, String profilePicture){
                return new UserUpdateRequest(email,firstName,lastName,dateOfBirth,description,profilePicture);
        }
}
