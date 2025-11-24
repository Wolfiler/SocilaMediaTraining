package com.socialmediatraining.dtoutils.dto;

public record UserCommentNotification(
        String userId,
        String userCommenterId,
        String userCommenterUsername,
        String commentId,
        String originalPostTrimContent
) {
    public static UserCommentNotification create(String userId,
                                                 String userCommenterId,String userCommenterUsername,
                                                 String commentId,String originalPostTrimContent){
        return new UserCommentNotification(
                userId,
                userCommenterId,
                userCommenterUsername,
                commentId,
                originalPostTrimContent
        );
    }
}
