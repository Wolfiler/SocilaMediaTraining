package com.socialmediatraining.contentservice.unittests.dto.post;

import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ContentResponseAdminTest {

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
                LocalDateTime.now(),
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

        ContentResponseAdmin contentResponse = ContentResponseAdmin.fromEntity(contentTest);

        assertThat(contentResponse.postResponse().id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.postResponse().creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.postResponse().parentId()).isEqualTo(contentTest.getParentId());
        assertThat(contentResponse.postResponse().created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.postResponse().updated_at()).isEqualTo(contentTest.getUpdatedAt());
        assertThat(contentResponse.postResponse().text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.postResponse().media_urls()).isEqualTo(contentTest.getMediaUrls());
        assertThat(contentResponse.deletedAt()).isEqualTo(contentTest.getDeletedAt());
    }

    @Test
    void fromEntity_ShouldHandleNullValues(){
        setupNullContent();

        ContentResponseAdmin contentResponse = ContentResponseAdmin.fromEntity(contentTest);

        assertThat(contentResponse).isNotNull();
        assertThat(contentResponse.postResponse()).isNotNull();
        assertThat(contentResponse.postResponse().id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.postResponse().creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.postResponse().parentId()).isNull();
        assertThat(contentResponse.postResponse().created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.postResponse().updated_at()).isNull();
        assertThat(contentResponse.postResponse().text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.postResponse().media_urls()).isNotNull();
        assertThat(contentResponse.postResponse().media_urls().size()).isEqualTo(0);
        assertThat(contentResponse.deletedAt()).isEqualTo(contentTest.getDeletedAt());
        assertThat(contentResponse.deletedAt()).isNull();
    }

    @Test
    void create_ShouldMapAllFieldsCorrectly(){
        setupContent();

        ContentResponseAdmin contentResponse = ContentResponseAdmin.create(
                contentTest.getId(),
                contentTest.getCreatorId(),
                contentTest.getParentId(),
                contentTest.getCreatedAt(),
                contentTest.getUpdatedAt(),
                contentTest.getText(),
                contentTest.getMediaUrls(),
                contentTest.getDeletedAt()
        );

        assertThat(contentResponse.postResponse().id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.postResponse().creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.postResponse().parentId()).isEqualTo(contentTest.getParentId());
        assertThat(contentResponse.postResponse().created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.postResponse().updated_at()).isEqualTo(contentTest.getUpdatedAt());
        assertThat(contentResponse.postResponse().text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.postResponse().media_urls()).isEqualTo(contentTest.getMediaUrls());
        assertThat(contentResponse.deletedAt()).isEqualTo(contentTest.getDeletedAt());
    }

    @Test
    void create_ShouldHandleNullValues(){
        setupNullContent();

        ContentResponseAdmin contentResponse = ContentResponseAdmin.create(
                contentTest.getId(),
                contentTest.getCreatorId(),
                contentTest.getParentId(),
                contentTest.getCreatedAt(),
                contentTest.getUpdatedAt(),
                contentTest.getText(),
                contentTest.getMediaUrls(),
                contentTest.getDeletedAt()
        );

        assertThat(contentResponse).isNotNull();
        assertThat(contentResponse.postResponse()).isNotNull();
        assertThat(contentResponse.postResponse().id()).isEqualTo(contentTest.getId());
        assertThat(contentResponse.postResponse().creator_id()).isEqualTo(contentTest.getCreatorId());
        assertThat(contentResponse.postResponse().parentId()).isNull();
        assertThat(contentResponse.postResponse().created_at()).isEqualTo(contentTest.getCreatedAt());
        assertThat(contentResponse.postResponse().updated_at()).isNull();
        assertThat(contentResponse.postResponse().text()).isEqualTo(contentTest.getText());
        assertThat(contentResponse.postResponse().media_urls()).isNotNull();
        assertThat(contentResponse.postResponse().media_urls().size()).isEqualTo(0);
        assertThat(contentResponse.deletedAt()).isEqualTo(contentTest.getDeletedAt());
        assertThat(contentResponse.deletedAt()).isNull();
    }
}