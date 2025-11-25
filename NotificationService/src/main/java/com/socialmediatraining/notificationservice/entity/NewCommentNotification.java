package com.socialmediatraining.notificationservice.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "notifications")
@SuperBuilder
public class NewCommentNotification extends Notification{
    String commentId;
    String commentUserId;
    String commentUserUsername;
    String cutContent;
}
