package com.socialmediatraining.contentservice.dto.post;

import java.time.LocalDateTime;

public record ContentResponseAdmin(
        ContentResponse postResponse,
        LocalDateTime deletedAt
) {
}
