package com.socialmediatraining.contentservice.dto.post;

import java.util.Map;

public record PostRequest(
        String text,
        Map<String, String> media_urls
) {
}
