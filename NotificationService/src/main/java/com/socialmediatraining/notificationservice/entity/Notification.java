package com.socialmediatraining.notificationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @UuidGenerator
    UUID id;
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
        WELCOME, NEW_FOLLOW, LIKE, COMMENT
    }
}
