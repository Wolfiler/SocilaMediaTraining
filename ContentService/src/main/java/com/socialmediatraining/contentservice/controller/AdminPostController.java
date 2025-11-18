package com.socialmediatraining.contentservice.controller;

import com.socialmediatraining.contentservice.dto.post.PostResponse;
import com.socialmediatraining.contentservice.dto.post.PostResponseAdmin;
import com.socialmediatraining.contentservice.service.PostService;
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
@RequestMapping("/api/v1/posts/admin")
@PreAuthorize("@roleUtils.hasAnyAdminRole(authentication)")
public class AdminPostController {
    private final PostService postService;

    @Autowired
    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<List<PostResponseAdmin>> getAllPostsFromUsername(
            @PathVariable("username") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(postService.getAllPostsFromUser(username,pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(postService.getPostByIdWithDeleted(postId));
    }
}
