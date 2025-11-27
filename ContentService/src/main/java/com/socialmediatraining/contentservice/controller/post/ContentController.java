package com.socialmediatraining.contentservice.controller.post;

import com.socialmediatraining.authenticationcommons.JwtUtils;
import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Content Service - Content controller", description = "API for posts/comments related operations")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class ContentController {
    private final ContentService contentService;

    @Autowired
    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @Operation(summary = "Create a new post made by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post created")
    })
    @PostMapping("/posts")
    public ResponseEntity<ContentResponse> createPost(
            @RequestBody @Valid ContentRequest post,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createContent(authHeader, post));
    }

    @Operation(summary = "Update a post made by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Content updated")
    })
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ContentResponse> updatePost(
            @PathVariable("postId") UUID postId,
            @RequestBody @Valid ContentRequest postRequest,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.updateContent(postId, authHeader, postRequest));
    }

    @Operation(summary = "Delete a post made by the authenticated user")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable("postId") UUID postId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.softDeleteContent(postId, authHeader));
    }

    @Operation(summary = "Get a post information")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ContentResponse> getPostById(
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getVisibleContentById(postId));
    }

    @Operation(summary = "Get all posts made by a user, in form of PageResponse")
    @GetMapping("/profile/{username}/posts")
    public ResponseEntity<PageResponse<ContentResponse>> getAllPostsFromUsername(
            @PathVariable("username") String username,
            @RequestParam(defaultValue = "all")
            @Pattern(regexp = "^(?i)(all|post|comment)$",
                    message = "Invalid content type. Must be one of: all, post, comment")
            String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
            ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getAllVisibleContentFromUser(username,pageable,type));
    }

    @Operation(summary = "Get the authenticated user's feed")
    @GetMapping("/feed")
    public ResponseEntity<Flux<PageResponse<ContentResponse>>> getUserFeed(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.status(HttpStatus.OK).body(contentService.getUserFeed(JwtUtils.getUsernameFromAuthHeader(authHeader),authHeader,pageable));
    }
}
