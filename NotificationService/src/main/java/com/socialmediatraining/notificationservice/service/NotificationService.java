package com.socialmediatraining.notificationservice.service;

import com.socialmediatraining.authenticationcommons.JwtUtils;
import com.socialmediatraining.authenticationcommons.dto.SimpleUserDataObject;
import com.socialmediatraining.dtoutils.dto.UserCommentNotification;
import com.socialmediatraining.dtoutils.dto.UserFollowNotification;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.notificationservice.dto.NewCommentNotificationDto;
import com.socialmediatraining.notificationservice.dto.NewFollowerNotificationDto;
import com.socialmediatraining.notificationservice.dto.NotificationDto;
import com.socialmediatraining.notificationservice.entity.NewCommentNotification;
import com.socialmediatraining.notificationservice.entity.NewFollowerNotification;
import com.socialmediatraining.notificationservice.entity.Notification;
import com.socialmediatraining.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.socialmediatraining.notificationservice.entity.Notification.NotificationStatus.READ;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "created-new-user", groupId = "notification-service" )
    public Mono<Void> sendNewUserNotification(SimpleUserDataObject user){
        NotificationDto newUserNotification = NotificationDto.create(
                user.id(),
                "Welcome to the platform",
                String.format("Welcome to the platform %s ! " +
                        "\nWe hope you enjoy your time here, and that your are able to create meaningful connections." +
                        "\nYou can now start to explore the platform, and find new interesting people to follow." +
                        "\nHave a very nice day.",user.username()),
                Notification.NotificationType.WELCOME
        );
        Notification notification = NotificationDto.toNotification(newUserNotification);
        return notificationRepository.save(notification)
                .onErrorResume(Mono::error)
                .then();
    }

    @KafkaListener(topics = "new-follower", groupId = "notification-service" )
    public Mono<Void> newFollowerNotification(UserFollowNotification userFollowNotification){
        NewFollowerNotificationDto newFollower = NewFollowerNotificationDto.create(
                userFollowNotification.userId(),
                "New follower !",
                userFollowNotification.followerId(),
                userFollowNotification.followerUsername(),
                String.format("%s just followed you !",userFollowNotification.followerUsername()),
                Notification.NotificationType.NEW_FOLLOW
        );
        NewFollowerNotification notification = NewFollowerNotificationDto.toNotification(newFollower);
        return notificationRepository.save(notification)
                .onErrorResume(Mono::error)
                .then();
    }

    @KafkaListener(topics = "new-comment", groupId = "notification-service" )
    public Mono<Void> newCommentNotification(UserCommentNotification usercommentNotification){
        NewCommentNotificationDto newCommentNotification = NewCommentNotificationDto.create(
                usercommentNotification.userId(),
                "A user has made a comment on one of your posts",
                usercommentNotification.commentId(),
                usercommentNotification.userCommenterId(),
                usercommentNotification.userCommenterUsername(),
                usercommentNotification.originalPostTrimContent(),
                String.format("%s has commented on your posts \"%s\"",
                        usercommentNotification.userCommenterUsername(),usercommentNotification.originalPostTrimContent()),
                Notification.NotificationType.NEW_COMMENT
        );
        NewCommentNotification notification = NewCommentNotificationDto.toNotification(newCommentNotification);
        return notificationRepository.save(notification)
                .onErrorResume(Mono::error)
                .then();
    }

    public Mono<String> setNotificationToRead(String authHeader, String notificationId){
        String userId = JwtUtils.getSubIdFromAuthHeader(authHeader);
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                            if(!notification.getUserId().equals(userId)){
                                return Mono.error(new UserActionForbiddenException("Cannot mark as read the notification of another user"));
                            }
                            notification.setRead(READ);
                            notification.setReadAt(LocalDateTime.now());
                            return notificationRepository.save(notification);
                        })
                .thenReturn("Notification marked as read")
                .onErrorResume(Mono::error);
    }

    public Mono<String> deleteNotification(String authHeader, String notificationId){
        String userId = JwtUtils.getSubIdFromAuthHeader(authHeader);
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                    if(!notification.getUserId().equals(userId)){
                        return Mono.error(new UserActionForbiddenException("Cannot delete the notification of another user"));
                    }
                    return notificationRepository.delete(notification);
                })
                .thenReturn("Notification deleted successfully")
                .onErrorResume(Mono::error);
    }

}
