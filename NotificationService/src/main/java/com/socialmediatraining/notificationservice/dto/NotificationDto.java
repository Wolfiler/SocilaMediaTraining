package com.socialmediatraining.notificationservice.dto;

import com.socialmediatraining.notificationservice.entity.Notification;

import java.time.LocalDateTime;

public record NotificationDto(
        String id,
        String userId,
        String title,
        String content,
        Notification.NotificationType type,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        Notification.NotificationStatus read
) {
    public static NotificationDto create(String userId, String title,
                                         String content, Notification.NotificationType type
    ){
        return new NotificationDto(
                null,
                userId,
                title,
                content,
                type,
                LocalDateTime.now(),
                null,
                Notification.NotificationStatus.UNREAD
        );
    }

    public static Notification toNotification(NotificationDto dto){
        return Notification.builder()
                .userId(dto.userId)
                .title(dto.title)
                .content(dto.content)
                .type(dto.type)
                .createdAt(dto.createdAt)
                .readAt(dto.readAt)
                .read(dto.read)
                .build();
    }
}
