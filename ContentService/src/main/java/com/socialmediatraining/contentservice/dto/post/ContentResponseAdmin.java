package com.socialmediatraining.contentservice.dto.post;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ContentResponseAdmin(
        ContentResponse postResponse,
        LocalDateTime deletedAt
) {

    public static ContentResponseAdmin create(UUID id, UUID creator_id, UUID parentId, LocalDateTime created_at,
                                              LocalDateTime updated_at, String text, Map<String, String> media_urls,
                                              LocalDateTime deletedAt) {
        return new ContentResponseAdmin(
                ContentResponse.create(id, creator_id,parentId,created_at,updated_at,text,media_urls)
                ,deletedAt);
    }
}
