package com.socialmediatraining.dtoutils.dto;

public record SimpleUserDataObject(
        String userId,
        String username
) {

    public static SimpleUserDataObject create(String userId, String username) {
        return new SimpleUserDataObject(userId, username);
    }
}
