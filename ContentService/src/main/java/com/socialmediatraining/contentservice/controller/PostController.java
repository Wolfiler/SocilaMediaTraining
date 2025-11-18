package com.socialmediatraining.contentservice.controller;

import com.socialmediatraining.contentservice.dto.post.PostRequest;
import com.socialmediatraining.contentservice.dto.post.PostResponse;
import com.socialmediatraining.contentservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
public class PostController {
    //TODO add caching

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    //region POST REGION
    //==================================================================================================================
    @PostMapping("/")
    public ResponseEntity<PostResponse> createPost(
            @RequestBody @Valid PostRequest post,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createNewPost(authHeader, post));
    }

    @GetMapping("/profile/{username}")//Profile and posts should probably be reversed
    public ResponseEntity<List<PostResponse>> getAllPostsFromUsername(
            @PathVariable("username") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(postService.getAllVisiblePostsFromUser(username,pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.getVisiblePostById(postId));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable("postId") UUID postId,
            @RequestBody @Valid PostRequest postRequest,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.updatePost(postId, authHeader, postRequest));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable("postId") UUID postId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.softDeletePost(postId, authHeader));
    }
    //==================================================================================================================
    //endregion

    //region LIKE REGION
    //==================================================================================================================
    @PostMapping("/like/{postId}")
    public ResponseEntity<String> likePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.likePost(authHeader, postId));
    }

    @GetMapping("/profile/{username}/like")
    public ResponseEntity<List<PostResponse>> getAllLikedPosts(
            @PathVariable("username") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(postService.getAllLikedPostsByUser(username,pageable));
    }

    @DeleteMapping("/like/{postId}")
    public ResponseEntity<String> deleteLike(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.deleteLike(authHeader, postId));
    }
    //==================================================================================================================
    //endregion
}
