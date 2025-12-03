package com.socialmediatraining.contentservice.unittests.entity;

import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

//Favorite are not tested because they are not implemented
class ContentTest {
    private UUID id;
    private UUID creatorId;
    private UUID parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String text;
    private Map<String, String> mediaUrls = new HashMap<>();
    private LocalDateTime deletedAt;
    private Set<UserContentLike> likes = new LinkedHashSet<>() {};
    private Content contentTest;
    private Validator validator;

    @BeforeEach
    void setupVariables(){
        id = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2025,7,14,12,0,0);
        updatedAt = LocalDateTime.of(2025,7,31,14,37,6);
        text = "This is a content test text";
        mediaUrls = new HashMap<>();
        deletedAt = null;
        likes = new LinkedHashSet<>() {};
    }

    void setupContent(){
        contentTest = Content.builder()
                .id(id)
                .creatorId(creatorId)
                .parentId(parentId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .text(text)
                .mediaUrls(mediaUrls)
                .deletedAt(deletedAt)
                .likes(likes)
                .build();
    }

    void setupValidator(){
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_ShouldCorrectlyMapAllFields(){
        setupContent();

        assertThat(contentTest.getId()).isEqualTo(id);
        assertThat(contentTest.getCreatorId()).isEqualTo(creatorId);
        assertThat(contentTest.getParentId()).isEqualTo(parentId);
        assertThat(contentTest.getCreatedAt()).isEqualTo(createdAt);
        assertThat(contentTest.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(contentTest.getText()).isEqualTo(text);
        assertThat(contentTest.getMediaUrls()).isEqualTo(mediaUrls);
        assertThat(contentTest.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(contentTest.getLikes()).isEqualTo(likes);
    }

    @Test
    void like_ShouldAddLikeToContent(){
        setupContent();

        assertThat(contentTest.getLikes().size()).isEqualTo(0);
        contentTest.addLike(new UserContentLike());
        assertThat(contentTest.getLikes().size()).isEqualTo(1);
    }

    @Test
    void like_ShouldRemoveLikeFromContent(){
        ExternalUser user = new ExternalUser();
        UserContentLike externalUserLike = new UserContentLike(UUID.randomUUID(), user,contentTest,LocalDateTime.now());
        likes = new LinkedHashSet<>(){{add(externalUserLike);}};
        setupContent();

        assertThat(contentTest.getLikes().size()).isEqualTo(1);
        contentTest.removeLike(user);
        assertThat(contentTest.getLikes().size()).isEqualTo(0);
    }

    @Test
    void validation_ShouldFailWhenTextIsTooLong() {
        text = "1234567890".repeat(25) + "123456";
        setupContent();
        setupValidator();

        Set<ConstraintViolation<Content>> violations = validator.validate(contentTest);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage()).contains("size must be between 1 and 255");
    }

    @Test
    void validation_ShouldFailWhenTextIsTooShort() {
        text = "";
        setupContent();
        setupValidator();

        Set<ConstraintViolation<Content>> violations = validator.validate(contentTest);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage()).contains("size must be between 1 and 255");
    }

    @Test
    void validation_ShouldFailWhenCreatorIdIsNull() {
        creatorId = null;
        setupContent();
        setupValidator();

        Set<ConstraintViolation<Content>> violations = validator.validate(contentTest);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    void addLike_ShouldNotAddDuplicateLikes() {
        setupContent();

        assertThat(contentTest.getLikes().size()).isEqualTo(0);
        contentTest.addLike(new UserContentLike());
        contentTest.addLike(new UserContentLike());
        assertThat(contentTest.getLikes().size()).isEqualTo(1);
    }

    @Test
    void removeLike_ShouldDoNothingWhenLikeDoesNotExist() {
        ExternalUser user = new ExternalUser();
        setupContent();

        assertDoesNotThrow(() -> contentTest.removeLike(user));
    }

    @Test
    void addLike_ShouldThrowWhenLikeIsNull() {
        setupContent();
        Exception exception = assertThrows(NullPointerException.class, () -> contentTest.addLike(null));
        String expectedMessage = "cannot be null";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).contains(expectedMessage);
    }

    @Test
    void removeLike_ShouldHandleNullUser() {
        ExternalUser user = new ExternalUser();
        UserContentLike externalUserLike = new UserContentLike(UUID.randomUUID(), user,contentTest,LocalDateTime.now());
        likes = new LinkedHashSet<>(){{add(externalUserLike);}};
        setupContent();

        assertThat(contentTest.getLikes().size()).isEqualTo(1);
        assertDoesNotThrow(() -> contentTest.removeLike(null));
        assertThat(contentTest.getLikes().size()).isEqualTo(1);
    }

    @Test
    void shouldNotAllowDirectModificationOfLikes() {
        Content content = Content.builder()
                .creatorId(creatorId)
                .text(text)
                .build();

        Set<UserContentLike> likes = content.getLikes();
        UserContentLike like = new UserContentLike();

        assertThrows(UnsupportedOperationException.class,() -> likes.add(like));
    }
}