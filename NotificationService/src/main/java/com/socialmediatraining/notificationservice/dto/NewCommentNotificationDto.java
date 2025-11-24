package com.socialmediatraining.notificationservice.dto;

import com.socialmediatraining.notificationservice.entity.NewCommentNotification;
import com.socialmediatraining.notificationservice.entity.Notification;

public record NewCommentNotificationDto (
        NotificationDto notification,
        String commentId,
        String commentUserId,
        String commentUserUsername,
        String cutContent
) {
        public static NewCommentNotificationDto create(String userId, String title,
                                                       String commentId,String commentUserId,String commentUserUsername,
                                                       String cutContent, String content, Notification.NotificationType type){
            return new NewCommentNotificationDto(
                    NotificationDto.create(userId, title, content, type),
                    commentId,
                    commentUserId,
                    commentUserUsername,
                    cutContent
            );
        }

        public static NewCommentNotification toNotification(NewCommentNotificationDto dto) {
            return NewCommentNotification.builder()
                    .userId(dto.notification.userId())
                    .commentId(dto.commentId())
                    .commentUserId(dto.commentUserId())
                    .commentUserUsername(dto.commentUserUsername())
                    .cutContent(dto.cutContent())
                    .title(dto.notification.title())
                    .content(dto.notification.content())
                    .type(dto.notification.type())
                    .createdAt(dto.notification.createdAt())
                    .readAt(dto.notification.readAt())
                    .read(dto.notification.read())
                    .build();
        }
}
