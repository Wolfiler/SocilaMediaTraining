package com.socialmediatraining.notificationservice.repository;

import com.socialmediatraining.notificationservice.entity.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

}
