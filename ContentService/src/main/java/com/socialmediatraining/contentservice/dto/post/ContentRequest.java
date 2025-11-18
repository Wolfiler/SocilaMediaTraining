package com.socialmediatraining.contentservice.dto.post;

import java.util.Map;
import java.util.UUID;

public record ContentRequest(
        UUID parentId,
        String text,
        Map<String, String> media_urls
) {
}
