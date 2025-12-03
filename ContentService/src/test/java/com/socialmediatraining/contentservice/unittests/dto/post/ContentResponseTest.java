package com.socialmediatraining.contentservice.unittests.dto.post;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ContentResponseTest {

    private Content contentTest;

    void setupContent(){
        contentTest = new Content(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                null,
                "This is a test content text",
                new HashMap<>(){
                    {
                        put("media", "https://example.com/image.jpg");
                        put("media2", "https://example2.com/image.png");
                    }},
                null,
                new LinkedHashSet<>() {},
                new LinkedHashSet<>() {}
        );
    }

    void setupNullContent(){
        contentTest = new Content(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now(),
                null,
                "test",
                null,
                null,
                null,
                null
        );
    }

    @Test
    void fromEntity_ShouldMapAllFieldsCorrectly(){
        setupContent();

        ContentResponse contentResponse = ContentResponse.fromEntity(contentTest);

        assertThat(contentResponse.id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.parentId()).isEqualTo(contentTest.getParentId());
        assertThat(contentResponse.created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.updated_at()).isEqualTo(contentTest.getUpdatedAt());
        assertThat(contentResponse.text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.media_urls()).isEqualTo(contentTest.getMediaUrls());
    }


    @Test
    void fromEntity_ShouldHandleNullValues(){
        setupNullContent();

        ContentResponse contentResponse = ContentResponse.fromEntity(contentTest);

        assertThat(contentResponse).isNotNull();
        assertThat(contentResponse.id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.parentId()).isNull();
        assertThat(contentResponse.created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.updated_at()).isNull();
        assertThat(contentResponse.text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.media_urls()).isNotNull();
        assertThat(contentResponse.media_urls().size()).isEqualTo(0);
    }

    @Test
    void create_ShouldMapAllFieldsCorrectly(){
        setupContent();

        ContentResponse contentResponse = ContentResponse.create(
                contentTest.getId(),
                contentTest.getCreatorId(),
                contentTest.getParentId(),
                contentTest.getCreatedAt(),
                contentTest.getUpdatedAt(),
                contentTest.getText(),
                contentTest.getMediaUrls()
        );

        assertThat(contentResponse.id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.parentId()).isEqualTo(contentTest.getParentId());
        assertThat(contentResponse.created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.updated_at()).isEqualTo(contentTest.getUpdatedAt());
        assertThat(contentResponse.text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.media_urls()).isEqualTo(contentTest.getMediaUrls());
    }

    @Test
    void create_ShouldHandleNullValues(){
        setupNullContent();

        ContentResponse contentResponse = ContentResponse.create(
                contentTest.getId(),
                contentTest.getCreatorId(),
                contentTest.getParentId(),
                contentTest.getCreatedAt(),
                contentTest.getUpdatedAt(),
                contentTest.getText(),
                contentTest.getMediaUrls()
        );

        assertThat(contentResponse).isNotNull();
        assertThat(contentResponse.id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.parentId()).isNull();
        assertThat(contentResponse.created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.updated_at()).isNull();
        assertThat(contentResponse.text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.media_urls()).isNotNull();
        assertThat(contentResponse.media_urls().size()).isEqualTo(0);
    }
}