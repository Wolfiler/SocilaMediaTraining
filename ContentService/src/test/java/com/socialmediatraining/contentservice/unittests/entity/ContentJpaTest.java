package com.socialmediatraining.contentservice.unittests.entity;

import com.socialmediatraining.contentservice.entity.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class ContentJpaTest {

    private UUID creatorId;
    private UUID parentId;
    private String text;
    private Map<String, String> mediaUrls = new HashMap<>();
    private Content contentTest;
    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setupVariables(){
        creatorId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        text = "This is a content test text";
        mediaUrls = new HashMap<>();
    }

    @Test
    void prePersist_ShouldSetCreatedAtAndUpdatedAt() {
        contentTest = Content.builder()
                .creatorId(creatorId)
                .parentId(parentId)
                .text(text)
                .mediaUrls(mediaUrls)
                .build();

        Content savedContent = entityManager.persistAndFlush(contentTest);

        assertThat(savedContent.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(savedContent.getUpdatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void preUpdate_ShouldUpdateUpdatedAt() throws InterruptedException {
        contentTest = Content.builder()
                .creatorId(creatorId)
                .parentId(parentId)
                .text(text)
                .mediaUrls(mediaUrls)
                .build();

        contentTest = entityManager.persistAndFlush(contentTest);
        Thread.sleep(1000);
        contentTest.setText("Updated text");
        entityManager.persistAndFlush(contentTest);

        assertThat(contentTest.getText()).isEqualTo("Updated text");
        assertThat(contentTest.getUpdatedAt()).isNotNull().isAfterOrEqualTo(contentTest.getCreatedAt().plusSeconds(1));
    }
}
