package com.socialmediatraining.contentservice.unittests.dto.post;

import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ContentRequestTest {

    private UUID parentId = null;
    private String text = null;
    private Map<String, String> media_urls = null;
    private Validator validator;

    @BeforeEach
    void setup(){
        parentId = UUID.randomUUID();
        text = "This is a random text for testing";
        media_urls = new HashMap<>();
        media_urls.put("image", "https://example.com/image.jpg");
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_ShouldCreateContentRequest() {
        ContentRequest newContentResponse = ContentRequest.create(parentId,text,media_urls);

        Set<ConstraintViolation<ContentRequest>> violations = validator.validate(newContentResponse);

        assertThat(newContentResponse.parentId()).isEqualTo(parentId);
        assertThat(newContentResponse.text()).isEqualTo(text);
        assertThat(newContentResponse.media_urls()).isEqualTo(media_urls);
        assertThat(violations.size()).isEqualTo(0);
    }

    @Test
    void validation_ShouldFailWhenContentIsEmpty(){
        text = "";
        ContentRequest newContentResponse = ContentRequest.create(parentId,text,media_urls);

        Set<ConstraintViolation<ContentRequest>> violations = validator.validate(newContentResponse);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage()).contains("size must be between 1 and 255");
    }

    @Test
    void validation_ShouldFailWhenContentIsTooLong(){
        text = "1234567890".repeat(25) + "123456";
        ContentRequest newContentResponse = ContentRequest.create(parentId,text,media_urls);
        assertThat(text.length()).isEqualTo(256);
        Set<ConstraintViolation<ContentRequest>> violations = validator.validate(newContentResponse);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(violations.iterator().next().getMessage()).contains("size must be between 1 and 255");
    }
}