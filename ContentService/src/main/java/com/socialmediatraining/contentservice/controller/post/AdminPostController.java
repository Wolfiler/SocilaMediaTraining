package com.socialmediatraining.contentservice.controller.post;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    private final ContentService contentService;

    @Autowired
    public AdminPostController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<PageResponse<ContentResponseAdmin>> getAllPostsFromUsername(
            @PathVariable("username") String username,
            @Pattern(regexp = "^(?i)(all|post|comment)$",
                    message = "Invalid content type. Must be one of: all, post, comment")
            String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getAllContentFromUser(username,pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ContentResponse> getPostById(@PathVariable("postId") UUID postId) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getContentByIdWithDeleted(postId));
    }
}
