package com.socialmediatraining.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
@Document(collection = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator
    String id;
    String userId;
    String title;
    String content;
    NotificationType type;
    @CreationTimestamp
    LocalDateTime createdAt;
    LocalDateTime readAt;
    NotificationStatus read;

    public enum NotificationStatus{
        UNREAD,READ
    }

    public enum NotificationType{
        WELCOME, NEW_FOLLOW, LIKE, NEW_COMMENT
    }

}
