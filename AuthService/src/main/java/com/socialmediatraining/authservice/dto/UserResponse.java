package com.socialmediatraining.authservice.dto;

public record UserResponse(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        String dateOfBirth,
        String description,
        String profilePicture
) {
}
