package com.socialmediatraining.contentservice.dto.post;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID creator_id,
        UUID parentId,
        LocalDateTime created_at,
        LocalDateTime updated_at,
        String text,
        Map<String, String> media_urls
) {
}
