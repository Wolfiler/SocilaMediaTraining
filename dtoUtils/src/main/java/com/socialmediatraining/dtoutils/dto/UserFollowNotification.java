package com.socialmediatraining.dtoutils.dto;

public record UserFollowNotification(
        String userId,
        String userUsername,
        String followerId,
        String followerUsername
) {

    public static UserFollowNotification create(String userId,String userUsername,
                                                String followerId,String followerUsername){
        return new UserFollowNotification(
                userId,
                userUsername,
                followerId,
                followerUsername
        );
    }
}
