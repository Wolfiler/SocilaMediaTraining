package com.socialmediatraining.notificationservice.entity;

import lombok.*;

import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "notifications")
@SuperBuilder
public class NewFollowerNotification extends Notification {
    public String followerId;
    public String followerUsername;
}
