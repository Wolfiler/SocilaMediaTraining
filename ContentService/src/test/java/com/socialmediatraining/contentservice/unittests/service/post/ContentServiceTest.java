package com.socialmediatraining.contentservice.unittests.service.post;

import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.contentservice.service.user.UserCacheService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentServiceTest {
    private static final String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkw" +
            "IiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5" +
            "zb2NpYWxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";
    private static final String USERNAME = "testuser";
    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserCacheService userCacheService;
    @Mock
    private KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate;
    @InjectMocks
    @Spy
    private ContentService contentService;
    private Content testContent;
    private SimpleUserDataObject testUserData;
    private Map<String, String> testMediaUrls;

    @BeforeEach
    void setUp() {
        UUID contentId = UUID.randomUUID();
        UUID creatorId = UUID.fromString(USER_ID);

        testContent = new Content();
        testContent.setId(contentId);
        testContent.setCreatorId(creatorId);
        testContent.setText("Test content");
        testContent.setCreatedAt(LocalDateTime.now());
        testContent.setUpdatedAt(LocalDateTime.now());

        testUserData = new SimpleUserDataObject(USER_ID, USERNAME);
        testMediaUrls = Map.of("image1", "http://example.com/image1.jpg");
    }

    @Test
    void createContent_WhenValidInput_ShouldCreateContent() {
        ContentRequest request = new ContentRequest(null, "Test content", testMediaUrls);
        given(userCacheService.getOrCreatNewExternalUserIfNotExists(anyString(), anyString())).willReturn(testUserData);
        given(contentRepository.save(any(Content.class))).willReturn(testContent);

        ContentResponse response = contentService.createContent(VALID_HEADER, request);

        assertThat(response).isNotNull();
        assertThat(testContent.getId()).isEqualTo(response.id());
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    void createContent_WhenParentIdNotExists_ShouldThrowException() {
        UUID parentId = UUID.randomUUID();
        ContentRequest request = new ContentRequest(parentId, "Test content", testMediaUrls);
        when(userCacheService.getOrCreatNewExternalUserIfNotExists(anyString(), anyString())).thenReturn(testUserData);
        when(contentRepository.existsByIdAndDeletedAtIsNull(parentId)).thenReturn(false);

        assertThrows(PostNotFoundException.class, () -> contentService.createContent(VALID_HEADER, request));
    }

    @Test
    void updateContent_WhenUserIsCreator_ShouldUpdateContent() {
        UUID contentId = UUID.randomUUID();
        ContentRequest request = new ContentRequest(null, "Updated content", testMediaUrls);
        testContent.setId(contentId);

        when(userCacheService.getUserDataByUsername(anyString())).thenReturn(testUserData);
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(testContent));
        when(contentRepository.save(any(Content.class))).thenReturn(testContent);

        ContentResponse response = contentService.updateContent(contentId, VALID_HEADER, request);

        assertThat(response).isNotNull();
        assertThat("Updated content").isEqualTo(response.text());
        verify(contentRepository).save(testContent);
    }

    @Test
    void updateContent_WhenUserNotCreator_ShouldThrowException() {
        UUID contentId = UUID.randomUUID();
        ContentRequest request = new ContentRequest(null, "Updated content", testMediaUrls);
        SimpleUserDataObject otherUser = new SimpleUserDataObject(UUID.randomUUID().toString(), "otheruser");

        when(userCacheService.getUserDataByUsername(anyString())).thenReturn(otherUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(testContent));

        assertThrows(UserActionForbiddenException.class, () ->
                contentService.updateContent(contentId, VALID_HEADER, request));
    }

    @Test
    void softDeleteContent_WhenUserIsCreator_ShouldSoftDelete() {
        UUID contentId = UUID.randomUUID();
        testContent.setId(contentId);

        when(userCacheService.getOrCreatNewExternalUserIfNotExists(anyString(), anyString())).thenReturn(testUserData);
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(testContent));
        when(contentRepository.save(any(Content.class))).thenReturn(testContent);

        String result = contentService.softDeleteContent(contentId, VALID_HEADER);

        assertThat(result).contains("deleted successfully");
        assertThat(testContent.getDeletedAt()).isNotNull();
        assertThat(testContent.getText()).isEqualTo("Deleted");
        verify(contentRepository).save(testContent);
    }

    @Test
    void getVisibleContentById_WhenContentExists_ShouldReturnContent() {
        UUID contentId = UUID.randomUUID();
        testContent.setId(contentId);

        given(contentRepository.findByIdAndDeletedAtIsNull(contentId)).willReturn(Optional.of(testContent));

        ContentResponse response = contentService.getVisibleContentById(contentId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(contentId);
    }

    @Test
    void getVisibleContentById_WhenContentNotExists_ShouldThrowException() {
        UUID contentId = UUID.randomUUID();
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> contentService.getVisibleContentById(contentId));
    }

    @Test
    void getContentByIdWithDeleted_WhenContentExists_ShouldReturnContent() {
        UUID contentId = UUID.randomUUID();
        testContent.setId(contentId);
        testContent.setDeletedAt(LocalDateTime.now());

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(testContent));

        ContentResponseAdmin response = contentService.getContentByIdWithDeleted(contentId);

        assertThat(response).isNotNull();
        assertThat(response.postResponse().id()).isEqualTo(contentId);
    }

    @Test
    void getAllVisibleContentFromUser_WhenUserExists_ShouldReturnContent() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Content> contentPage = new PageImpl<>(List.of(testContent), pageable, 1);

        when(userCacheService.getUserDataByUsername(anyString())).thenReturn(testUserData);
        when(contentRepository.findAllByCreatorIdAndDeletedAtIsNull(any(UUID.class), any(Pageable.class)))
                .thenReturn(Optional.of(contentPage));

        PageResponse<ContentResponse> response =
                contentService.getAllVisibleContentFromUser(USERNAME, pageable, "all");

        assertThat(response).isNotNull();
        assertThat(response.content().size()).isEqualTo(1);
    }

    @Test
    void getUserFeed_WhenUserHasFollowedUsers_ShouldReturnFeed() {
        doReturn(Flux.just(
                new SimpleUserDataObject("user1", "testuser1"),
                new SimpleUserDataObject("user2", "testuser2")
        )).when(contentService).getListOfFollowedUser(anyString(), anyString());

        Content testContent = new Content();
        testContent.setText("Test content");
        when(contentRepository.findAllByCreatorIdInAndDeletedAtIsNull(anyList(), any()))
                .thenReturn(Optional.of(new PageImpl<>(List.of(testContent))));

        Flux<PageResponse<ContentResponse>> result = contentService.getUserFeed(
                "testuser",
                "Bearer token",
                PageRequest.of(0, 10)
        );

        StepVerifier.create(result)
                .expectNextMatches(page ->
                        page.content().size() == 1 &&
                                "Test content".equals(page.content().getFirst().text())
                )
                .verifyComplete();
    }

    @Test
    void createContent_WhenNullHeader_ShouldThrowException() {
        ContentRequest request = new ContentRequest(null, "Test", testMediaUrls);
        assertThrows(IllegalArgumentException.class,
                () -> contentService.createContent(null, request));
    }

    @Test
    void createContent_WhenNullRequest_ShouldThrowException() {
        assertThrows(HttpServerErrorException.class,
                () -> contentService.createContent(VALID_HEADER, null));
    }

    @Test
    void createContent_WhenUserCacheFails_ShouldPropagateException() {
        ContentRequest request = new ContentRequest(null, "Test", testMediaUrls);
        when(userCacheService.getOrCreatNewExternalUserIfNotExists(anyString(), anyString()))
                .thenThrow(new RuntimeException("Cache service unavailable"));

        assertThrows(RuntimeException.class,
                () -> contentService.createContent(VALID_HEADER, request));
    }

    @Test
    void updateContent_WhenContentNotFound_ShouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        ContentRequest request = new ContentRequest(null, "Updated", testMediaUrls);

        when(contentRepository.findByIdAndDeletedAtIsNull(nonExistentId))
                .thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class,
                () -> contentService.updateContent(nonExistentId,VALID_HEADER, request));
    }

    @Test
    void deleteContent_ShouldSoftDelete() {
        UUID contentId = UUID.randomUUID();
        Content content = new Content();
        content.setId(contentId);
        content.setCreatorId(UUID.fromString(USER_ID));

        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(content));
        when(userCacheService.getOrCreatNewExternalUserIfNotExists(anyString(), anyString())).thenReturn(testUserData);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));

        contentService.softDeleteContent(contentId,VALID_HEADER);

        assertNotNull(content.getDeletedAt());
    }
}