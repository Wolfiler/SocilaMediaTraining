package com.socialmediatraining.userservice.controller;

import com.socialmediatraining.userservice.dto.ExternalUserResponse;
import com.socialmediatraining.userservice.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/follow")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class FollowController {

    private final FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{username}")
    public ResponseEntity<String> followUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.followUser(username, token));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> unfollowUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.OK).body(followService.unfollowUser(username, token));
    }

    @GetMapping("/followers/{username}")
    public ResponseEntity<Page<ExternalUserResponse>> getAllFollowersOfUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(followService.getAllFollowersOfUser(username,pageable));
    }

    @GetMapping("/follows/{username}")
    public ResponseEntity<Page<ExternalUserResponse>> getAllFollowOfUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(followService.getAllFollowOfUser(username,pageable));
    }
}
