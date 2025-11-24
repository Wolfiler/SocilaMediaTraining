package com.socialmediatraining.notificationservice.controller;

import com.socialmediatraining.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Mono<String>> readNotification(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String notificationId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.setNotificationToRead(authHeader,notificationId));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Mono<String>> deleteNotification(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String notificationId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.deleteNotification(authHeader,notificationId));
    }

}
