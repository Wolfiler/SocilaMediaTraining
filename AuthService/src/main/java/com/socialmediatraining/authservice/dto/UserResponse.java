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

    public static UserResponse create(String id,String username,String firstName,String lastName,String email,
                                      String dateOfBirth, String description, String profilePicture){
        return new UserResponse(
                id,
                username,
                firstName,
                lastName,
                email,
                dateOfBirth,
                description,
                profilePicture
        );
    }
}
