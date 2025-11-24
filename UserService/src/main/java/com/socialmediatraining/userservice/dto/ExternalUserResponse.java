package com.socialmediatraining.userservice.dto;

public record ExternalUserResponse(
        String userId,
        String username
) {
    public static ExternalUserResponse create(String userId, String username) {
        return new ExternalUserResponse(userId, username);
    }
}
