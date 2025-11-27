package com.socialmediatraining.contentservice.controller.like;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.service.like.LikeService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/like")
@Tag(name = "Content Service - Like controller", description = "API for like operations")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class LikeController {

    private final LikeService likeService;

    @Autowired
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @Operation(summary = "Create a like between authenticated user and given content (post or comment) id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Content liked")
    })
    @PostMapping("/{postId}")
    public ResponseEntity<String> likePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(likeService.likeContent(authHeader, postId));
    }

    @Operation(summary = "Get all content (post or comment) liked by given user, in form of PageResponse")
    @GetMapping("/profile/{username}")
    public ResponseEntity<PageResponse<ContentResponse>> getAllLikedPosts(
            @PathVariable("username") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(likeService.getAllLikedContentsByUser(username,pageable));
    }

    @Operation(summary = "Delete a like between authenticated user and given content (post or comment) id")
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deleteLike(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(likeService.deleteLike(authHeader, postId));
    }
}
