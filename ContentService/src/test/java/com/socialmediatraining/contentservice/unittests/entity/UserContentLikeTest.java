package com.socialmediatraining.contentservice.unittests.entity;

import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UserContentLikeTest {

    private UUID id = null;
    private ExternalUser user = null;
    private Content content = null;

    @BeforeEach
    void setupVariables(){
        id = UUID.randomUUID();

        user = ExternalUser.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();

        content = Content.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text("This is a content text test")
                .mediaUrls(new HashMap<>())
                .build();
    }


    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        UserContentLike userContentLike = UserContentLike.builder()
                .id(id)
                .user(user)
                .content(content)
                .build();

        assertThat(userContentLike.getId()).isEqualTo(id);
        assertThat(userContentLike.getUser()).isEqualTo(user);
        assertThat(userContentLike.getContent()).isEqualTo(content);
    }

    @Test
    void builder_shouldHandleNullFields() {
        UserContentLike userContentLike = UserContentLike.builder()
                .id(null)
                .user(null)
                .content(null)
                .build();

        assertThat(userContentLike.getId()).isNull();
        assertThat(userContentLike.getUser()).isNull();
        assertThat(userContentLike.getContent()).isNull();
    }

    @Test
    void setUser_withNull_shouldClearUserAndContentRelationship() {
        UserContentLike userContentLike = user.addContentLike(content);
        userContentLike.setUser(null);

        assertThat(user.getLikes().contains(userContentLike)).isFalse();
        assertThat(content.getLikes().contains(userContentLike)).isFalse();
        assertThat(userContentLike.getUser()).isEqualTo(null);
        assertThat(userContentLike.getContent()).isEqualTo(null);
    }

    @Test
    void setContent_withNull_shouldClearUserAndContentRelationship() {
        UserContentLike userContentLike = user.addContentLike(content);
        userContentLike.setContent(null);

        assertThat(user.getLikes().contains(userContentLike)).isFalse();
        assertThat(content.getLikes().contains(userContentLike)).isFalse();
        assertThat(userContentLike.getUser()).isEqualTo(null);
        assertThat(userContentLike.getContent()).isEqualTo(null);
    }
}