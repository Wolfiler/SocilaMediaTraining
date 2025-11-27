package com.socialmediatraining.userservice.controller;

import com.socialmediatraining.userservice.dto.ExternalUserResponse;
import com.socialmediatraining.userservice.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follow")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
@Validated
@Tag(name = "User Service - Follow controller", description = "API for following/unfollowing related operations")
public class FollowController {

    private final FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "Authenticated user follow given user, by their username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Follow successfull")
    })
    @PostMapping("/{username}")
    public ResponseEntity<String> followUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.followUser(username, token));
    }

    @Operation(summary = "Authenticated user unfollow given user, by their username")
    @DeleteMapping("/{username}")
    public ResponseEntity<String> unfollowUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.OK).body(followService.unfollowUser(username, token));
    }

    @Operation(summary = "Get all follower of given user")
    @GetMapping("/followers/{username}")
    public ResponseEntity<Page<ExternalUserResponse>> getAllFollowersOfUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(followService.getAllFollowersOfUser(username,pageable));
    }

    @Operation(summary = "Get all user following given user")
    @GetMapping("/follows/{username}")
    public ResponseEntity<List<ExternalUserResponse>> getAllFollowOfUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "50")
            @Min(value=1,message = "Limit should be at least 1")
            @Max(value=100,message = "Limit should be at most 100")
            int limit,
            @RequestParam(defaultValue = "none")
            @Pattern(regexp = "^(?i)(none|activity)$", message = "Invalid content type. Must be one of: none, activity")
             String orderBy
            ) {
        return ResponseEntity.status(HttpStatus.OK).body(followService
                .getAllFollowOfUser(username,limit, orderBy));
    }
}
