package com.socialmediatraining.contentservice.unittests.service.like;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.UserContentLikeRepository;
import com.socialmediatraining.contentservice.service.like.LikeService;
import com.socialmediatraining.contentservice.service.user.UserCacheService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {
    private final static String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5zb2NpY" +
            "WxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserContentLikeRepository userContentLikeRepository;
    @Mock
    private UserCacheService userCacheService;
    @InjectMocks
    private LikeService likeService;

    private ExternalUser testUser;
    private Content testContent;
    private SimpleUserDataObject userData;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        testUser = new ExternalUser();
        testUser.setId(id);
        testUser.setUsername("testuser");

        testContent = new Content();
        testContent.setId(UUID.randomUUID());
        testContent.setCreatedAt(LocalDateTime.now());

        userData = new SimpleUserDataObject(id.toString(), testUser.getUsername());
    }

    @Test
    void likeContent_WhenValidInput_ShouldReturnSuccessMessage() {
        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testContent));
        when(userContentLikeRepository.existsByUserIdAndContentId(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(userContentLikeRepository.save(any(UserContentLike.class))).thenReturn(new UserContentLike());

        String result = likeService.likeContent(VALID_HEADER, testContent.getId());

        assertThat(result).contains("liked post with userId");
        verify(userContentLikeRepository).save(any(UserContentLike.class));
    }

    @Test
    void likeContent_WhenPostNotFound_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () ->
                likeService.likeContent(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void likeContent_WhenAlreadyLiked_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testContent));
        when(userContentLikeRepository.existsByUserIdAndContentId(any(UUID.class), any(UUID.class))).thenReturn(true);

        assertThrows(UserActionForbiddenException.class, () ->
                likeService.likeContent(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void deleteLike_WhenValidInput_ShouldReturnSuccessMessage() {
        UserContentLike testLike = new UserContentLike();
        testLike.setUser(testUser);
        testLike.setContent(testContent);

        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testContent));
        when(userContentLikeRepository.findByContentAndUser_Id(any(Content.class), any(UUID.class)))
                .thenReturn(Optional.of(testLike));

        String result = likeService.deleteLike(VALID_HEADER, testContent.getId());

        assertThat(result).contains("unliked post with userId");
        verify(userCacheService).saveExternalUser(any(ExternalUser.class));
    }

    @Test
    void deleteLike_WhenPostNotFound_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () ->
                likeService.deleteLike(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void deleteLike_WhenLikeNotFound_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString())).thenReturn(testUser);
        when(contentRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).thenReturn(Optional.of(testContent));
        when(userContentLikeRepository.findByContentAndUser_Id(any(Content.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(UserActionForbiddenException.class, () ->
                likeService.deleteLike(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void getAllLikedContentsByUser_WhenUserHasLikes_ShouldReturnLikedContents() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userCacheService.getUserDataByUsername(anyString())).thenReturn(userData);
        when(userContentLikeRepository.findAllByUser_Id(any(UUID.class), any(Pageable.class)))
                .thenReturn(Optional.of(new PageImpl<>(List.of(createTestLike()))));

        PageResponse<ContentResponse> result = likeService.getAllLikedContentsByUser(testUser.getUsername(), pageable);

        assertThat(result.content()).hasSize(1);
    }

    private UserContentLike createTestLike() {
        UserContentLike like = new UserContentLike();
        like.setId(UUID.randomUUID());
        like.setUser(testUser);
        like.setContent(testContent);
        return like;
    }

    @Test
    void getAllLikedContentsByUser_WhenNoLikes_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userCacheService.getUserDataByUsername(anyString())).thenReturn(userData);
        when(userContentLikeRepository.findAllByUser_Id(any(UUID.class), any(Pageable.class)))
                .thenReturn(Optional.empty());

        PageResponse<ContentResponse> result = likeService.getAllLikedContentsByUser(testUser.getUsername(), pageable);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void likeContent_WhenUserNotFound_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString()))
                .thenThrow(new UserDoesntExistsException("User not found: " + testUser.getUsername()));

        assertThrows(UserDoesntExistsException.class, () ->
                likeService.likeContent(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void deleteLike_WhenUserNotFound_ShouldThrowException() {
        when(userCacheService.getExternalUserByUsername(anyString()))
                .thenThrow(new UserDoesntExistsException("User not found: " + testUser.getUsername()));

        assertThrows(UserDoesntExistsException.class, () ->
                likeService.deleteLike(VALID_HEADER, testContent.getId())
        );
    }

    @Test
    void getAllLikedContentsByUser_WhenUserDataNotFound_ShouldThrowException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userCacheService.getUserDataByUsername(anyString()))
                .thenThrow(new UserDoesntExistsException("User not found: " + testUser.getUsername()));

        assertThrows(UserDoesntExistsException.class, () ->
                likeService.getAllLikedContentsByUser("nonexistent", pageable)
        );
    }

    @Test
    void likeContent_WhenInvalidAuthHeader_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                likeService.likeContent("", testContent.getId())
        );

        assertThrows(IllegalArgumentException.class, () ->
                likeService.likeContent(null, testContent.getId())
        );
    }

    @Test
    void getAllLikedContentsByUser_WithInvalidPageable_ShouldHandleGracefully() {
        assertThrows(IllegalArgumentException.class, () ->
                likeService.getAllLikedContentsByUser(testUser.getUsername(), PageRequest.of(-1, 10))
        );

        assertThrows(IllegalArgumentException.class, () ->
                likeService.getAllLikedContentsByUser(testUser.getUsername(), PageRequest.of(0, 0))
        );
    }

    @Test
    void likeContent_WhenNullParameters_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                likeService.likeContent(null, null)
        );

        assertThrows(PostNotFoundException.class, () ->
                likeService.likeContent(VALID_HEADER, null)
        );
    }
}