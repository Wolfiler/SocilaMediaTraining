package com.socialmediatraining.contentservice.dto.post;

import java.util.Map;
import java.util.UUID;

public record ContentRequest(
        UUID parentId,
        String text,
        Map<String, String> media_urls
) {

    public static ContentRequest create(UUID parentId,String text,Map<String, String> media_urls){
        return new ContentRequest(parentId,text,media_urls);
    }
}
