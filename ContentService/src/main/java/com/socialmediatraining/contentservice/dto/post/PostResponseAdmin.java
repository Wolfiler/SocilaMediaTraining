package com.socialmediatraining.contentservice.dto.post;

import java.time.LocalDateTime;

public record PostResponseAdmin(
        PostResponse postResponse,
        LocalDateTime deletedAt
) {
}
