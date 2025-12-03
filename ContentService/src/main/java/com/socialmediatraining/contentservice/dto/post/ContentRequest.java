package com.socialmediatraining.contentservice.dto.post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record ContentRequest(
        UUID parentId,
        @Size(min = 1, max = 255)
        String text,
        Map<String, String> media_urls
) {

    public static ContentRequest create(UUID parentId,String text,Map<String, String> media_urls){
        return new ContentRequest(parentId,text,media_urls);
    }
}
