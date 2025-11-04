package com.socialmediatraining.authservice.dto;

public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String creationDate,
        String dateOfBirth,
        String description,
        String profilePicture
) {
}
