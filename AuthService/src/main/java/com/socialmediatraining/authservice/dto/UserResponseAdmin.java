package com.socialmediatraining.authservice.dto;

public record UserResponseAdmin(
        UserResponse userResponse,
        String creationDate

        ) {

        public UserResponseAdmin create(String id,String username,String firstName,String lastName,String email,
                                        String dateOfBirth, String description, String profilePicture,String creationDate){

                return new UserResponseAdmin(UserResponse.create(id,username,firstName,lastName,email,dateOfBirth
                        ,description,profilePicture),creationDate);
        }
}
