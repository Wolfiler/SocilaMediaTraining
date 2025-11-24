package com.socialmediatraining.notificationservice.entity;

import jakarta.persistence.Entity;
import lombok.*;

import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "notifications")
@SuperBuilder
public class NewFollowerNotification extends Notification {
    public String followerId;
    public String followerUsername;
}
