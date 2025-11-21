package com.socialmediatraining.notificationservice.dto;

import com.socialmediatraining.notificationservice.entity.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String userId,
        String title,
        String content,
        Notification.NotificationType type,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        Notification.NotificationStatus read
) {
    //TODO do the same for all records
    public static NotificationDto create(String userId, String title,
                                         String content, Notification.NotificationType type
    ){
        return new NotificationDto(
                UUID.randomUUID(),
                userId,
                title,
                content,
                type,
                LocalDateTime.now(),
                null,
                Notification.NotificationStatus.UNREAD
        );
    }
}
