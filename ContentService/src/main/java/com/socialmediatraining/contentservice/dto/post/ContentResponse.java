package com.socialmediatraining.contentservice.dto.post;

import com.socialmediatraining.contentservice.entity.Content;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ContentResponse(
        UUID id,
        UUID creator_id,
        UUID parentId,
        LocalDateTime created_at,
        LocalDateTime updated_at,
        String text,
        Map<String, String> media_urls
) {

    public static ContentResponse create(UUID id, UUID creator_id, UUID parentId, LocalDateTime created_at,
                                         LocalDateTime updated_at, String text, Map<String, String> media_urls){
        return new ContentResponse(
                id,
                creator_id,
                parentId,
                created_at,
                updated_at,
                text,
                media_urls);
    }

    public static ContentResponse fromEntity(Content content){
        return ContentResponse.create(
                content.getId(),
                content.getCreatorId(),
                content.getParentId(),
                content.getCreatedAt(),
                content.getUpdatedAt(),
                content.getText(),
                content.getMediaUrls());
    }
}
