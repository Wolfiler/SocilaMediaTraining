package com.socialmediatraining.contentservice.controller.post;

import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.service.post.ContentService;
import jakarta.validation.Valid;
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
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class ContentController {
    private final ContentService contentService;

    @Autowired
    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    //region CONTENT REGION
    //==================================================================================================================
    @PostMapping("/posts")
    public ResponseEntity<ContentResponse> createPost(
            @RequestBody @Valid ContentRequest post,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createContent(authHeader, post));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<ContentResponse> updatePost(
            @PathVariable("postId") UUID postId,
            @RequestBody @Valid ContentRequest postRequest,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.updateContent(postId, authHeader, postRequest));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable("postId") UUID postId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.softDeleteContent(postId, authHeader));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ContentResponse> getPostById(
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getVisibleContentById(postId));
    }

    @GetMapping("/profile/{username}/posts")
    public ResponseEntity<Page<ContentResponse>> getAllPostsFromUsername(
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

    //==================================================================================================================
    //endregion
}
