package com.socialmediatraining.notificationservice.dto;

import com.socialmediatraining.notificationservice.entity.NewFollowerNotification;
import com.socialmediatraining.notificationservice.entity.Notification;

public record NewFollowerNotificationDto(
        NotificationDto notification,
        String followerId,
        String followerUsername
) {
    public static NewFollowerNotificationDto create(String userId, String title,
                                                    String followerId,String followerUsername,
                                                    String content, Notification.NotificationType type
    ){
        return new NewFollowerNotificationDto(
                NotificationDto.create(userId, title, content, type),
                followerId,
                followerUsername
        );
    }

    public static NewFollowerNotification toNotification(NewFollowerNotificationDto dto) {
        return NewFollowerNotification.builder()
                .userId(dto.notification.userId())
                .followerId(dto.followerId)
                .followerUsername(dto.followerUsername)
                .title(dto.notification.title())
                .content(dto.notification.content())
                .type(dto.notification.type())
                .createdAt(dto.notification.createdAt())
                .readAt(dto.notification.readAt())
                .read(dto.notification.read())
                .build();
    }
}