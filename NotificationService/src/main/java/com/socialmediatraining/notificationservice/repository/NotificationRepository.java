package com.socialmediatraining.notificationservice.repository;

import com.socialmediatraining.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findAllByUserId(String userId, Pageable pageable);

    Flux<Notification> findAllByUserIdAndReadIn(String userId,String[] read ,Pageable pageable);
    Mono<Long> countByUserIdAndReadIn(String userId,String[] read);

}
