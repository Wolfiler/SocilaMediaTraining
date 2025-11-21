package com.socialmediatraining.notificationservice.service;

import com.netflix.discovery.converters.Auto;
import com.socialmediatraining.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    //Kafka consume new user welcome notif

    //Kafka consume new follower notif

    //Kafka consume new like content notif -> Might not want to do that

    //Kafka consume new comment on content notif

    //Change message type from unread to read

    //Delete notification
}
