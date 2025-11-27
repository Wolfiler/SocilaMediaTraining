package com.socialmediatraining.notificationservice.controller;

import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.notificationservice.entity.Notification;
import com.socialmediatraining.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.bson.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
@Tag(name = "Notification Service", description = "API for notification related operations")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Change the given notification status to READ (normally from UNREAD)")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Mono<String>> readNotification(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String notificationId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.setNotificationToRead(authHeader,notificationId));
    }

    @Operation(summary = "Delete the given authenticated user notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Mono<String>> deleteNotification(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String notificationId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(notificationService.deleteNotification(authHeader,notificationId));
    }

    @Operation(summary = "Get all notification of the user with the demanded status")
    @GetMapping("")
    public Mono<ResponseEntity<PageResponse<Notification>>> getAllNotification(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all")
            @Pattern(regexp = "^(?i)(all|unread|read)$",
                    message = "Invalid content type. Must be one of: all, unread, read")
            String status
    ){
        Pageable pageable = PageRequest.of(page, size);
        return notificationService.getAllNotifications(authHeader, pageable,status)
                .map(PageResponse::from)
                .map(ResponseEntity::ok);
    }
}
