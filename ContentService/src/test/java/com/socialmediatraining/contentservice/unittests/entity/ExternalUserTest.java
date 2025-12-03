package com.socialmediatraining.contentservice.unittests.entity;

import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

//Favorite are not tested because they are not implemented
class ExternalUserTest {

    private ExternalUser user;
    private Content content;

    @BeforeEach
    void setUpUser() {
        user = ExternalUser.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();
    }

    @BeforeEach
    void setUpContent() {
        content = Content.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text("This is a content text test")
                .mediaUrls(new HashMap<>())
                .deletedAt(null)
                .build();
    }


    @Test
    void builder_shouldCreateUserWithAllFields() {
        UUID id = UUID.randomUUID();
        String username = "testuser";

        ExternalUser user = ExternalUser.builder()
                .id(id)
                .username(username)
                .build();

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    void addContentLike_shouldAddLikeToContentAndUser() {
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        user.addContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(1);
    }

    @Test
    void addContentLike_shouldNotAddDuplicateLikes() {
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        user.addContentLike(content);
        user.addContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(1);
    }

    @Test
    void addContentLike_shouldThrowExceptionWhenContentIsNull() {
        assertThat(user.getLikes().size()).isEqualTo(0);
        Exception e = assertThrows(NullPointerException.class,
                () -> user.addContentLike(null));
        assertThat(e.getMessage()).contains("Content cannot be null");
    }

    @Test
    void removeContentLike_shouldRemoveLikeFromContentAndUser() {
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        user.addContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(1);
        user.removeContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(0);
    }

    @Test
    void removeContentLike_shouldHandleNonExistentLike() {
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        assertDoesNotThrow(() -> user.removeContentLike(content));
        assertThat(user.getLikes().size()).isEqualTo(0);
    }

    @Test
    void removeContentLike_shouldHandleNullContent() {
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        assertDoesNotThrow(() -> user.removeContentLike(null));
    }

    @Test
    void getContentLiked_shouldReturnUnmodifiableSet() {
        Set<UserContentLike> likes = content.getLikes();
        UserContentLike like = new UserContentLike();

        assertThrows(UnsupportedOperationException.class,() -> likes.add(like));
    }

    @Test
    void addContentLike_shouldSetBidirectionalRelationship(){
        assertThat(user.getLikes()).isNotNull();
        assertThat(user.getLikes().size()).isEqualTo(0);
        user.addContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(1);
        assertThat(content.getLikes().size()).isEqualTo(1);
    }

    @Test
    void removeContentLike_shouldRemoveBidirectionalRelationship(){
        user.addContentLike(content);
        user.removeContentLike(content);
        assertThat(user.getLikes().size()).isEqualTo(0);
        assertThat(content.getLikes().size()).isEqualTo(0);
    }
}