package com.socialmediatraining.contentservice.unittests.controller.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmediatraining.contentservice.controller.post.ContentController;
import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ContentService contentService;
    private Content testContent;
    private ContentResponse testResponse;
    private ContentRequest contentRequest;
    private final static String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5zb2NpY" +
            "WxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        testContent = Content.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .text("Test post content")
                .mediaUrls(Map.of("image", "http://example.com/image.jpg"))
                .build();
        testResponse = ContentResponse.fromEntity(testContent);
        contentRequest = new ContentRequest(
                testContent.getParentId(),
                testContent.getText(),
                testContent.getMediaUrls()
        );
    }

    //region <createPost>
    @Test
    void createPost_WithValidRequest_Returns201() throws Exception {
        ContentRequest request = new ContentRequest(
                testContent.getParentId(),
                testContent.getText(),
                testContent.getMediaUrls()
        );

        given(contentService.createContent(any(String.class), any(ContentRequest.class))).willReturn(testResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testContent.getId().toString()))
                .andExpect(jsonPath("$.text").value(testContent.getText()))
                .andExpect(jsonPath("$.media_urls.image").value(testContent.getMediaUrls().get("image")));
    }

    @Test
    void createPost_WithComment_Returns201() throws Exception {
        testContent.setParentId(UUID.randomUUID());
        testResponse = ContentResponse.fromEntity(testContent);

        given(contentService.createContent(any(String.class), any(ContentRequest.class))).willReturn(testResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(testContent.getParentId().toString()));
    }

    @Test
    void createPost_WithNonExistentParent_Returns404() throws Exception {
        contentRequest = new ContentRequest(
                UUID.randomUUID(),
                testContent.getText(),
                testContent.getMediaUrls()
        );
        String errorMessage = "Parent post with userId " + contentRequest.parentId() + " doesn't exists";

        given(contentService.createContent(any(String.class), any(ContentRequest.class)))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void createPost_WithoutAuthHeader_Returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_WithEmptyText_Returns400() throws Exception {
        contentRequest = new ContentRequest(
                UUID.randomUUID(),
                "",
                testContent.getMediaUrls()
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }
    //endregion

    //region <updatePost>
    @Test
    void updatePost_WithValidRequest_Returns201() throws Exception {
        ContentRequest updateRequest = new ContentRequest(
                null,
                "Updated post content",
                Map.of("image", "http://example.com/updated.jpg")
        );

        Content updatedContent = Content.builder()
                .id(testContent.getId())
                .creatorId(testContent.getCreatorId())
                .text(updateRequest.text())
                .mediaUrls(updateRequest.media_urls())
                .build();
        ContentResponse updatedResponse = ContentResponse.fromEntity(updatedContent);

        given(contentService.updateContent(any(UUID.class), any(String.class), any(ContentRequest.class)))
                .willReturn(updatedResponse);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testContent.getId().toString()))
                .andExpect(jsonPath("$.text").value(updateRequest.text()))
                .andExpect(jsonPath("$.media_urls.image").value(updateRequest.media_urls().get("image")));
    }

    @Test
    void updatePost_NonExistentPost_Returns404() throws Exception {
        UUID nonExistentPostId = UUID.randomUUID();
        String errorMessage = "Cannot find post with userId " + nonExistentPostId + " to edit";

        given(contentService.updateContent(any(UUID.class), any(String.class), any(ContentRequest.class)))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", nonExistentPostId)
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void updatePost_UnauthorizedUser_Returns403() throws Exception {
        String errorMessage = "User is not authorized to update the post of another post";

        given(contentService.updateContent(any(UUID.class), any(String.class), any(ContentRequest.class)))
                .willThrow(new UserActionForbiddenException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void updatePost_WithoutAuthHeader_Returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", testContent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(contentRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePost_WithEmptyText_Returns400() throws Exception {
        ContentRequest invalidRequest = new ContentRequest(null, "", Map.of());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }
    //endregion

    //region<deletePost>
    @Test
    void deletePost_WithValidRequest_Returns200() throws Exception {
        String successMessage = String.format("Post %s deleted successfully", testContent.getId());

        given(contentService.softDeleteContent(eq(testContent.getId()), any(String.class))).willReturn(successMessage);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string(successMessage));
    }

    @Test
    void deletePost_NonExistentPost_Returns404() throws Exception {
        UUID nonExistentPostId = UUID.randomUUID();
        String errorMessage = "Cannot find post with userId " + nonExistentPostId + " to delete";

        given(contentService.softDeleteContent(eq(nonExistentPostId), any(String.class)))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{postId}", nonExistentPostId)
                        .header("Authorization", VALID_HEADER))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void deletePost_AlreadyDeletedPost_Returns404() throws Exception {
        String errorMessage = "Cannot find post with userId " + testContent.getId() + " to delete";

        given(contentService.softDeleteContent(eq(testContent.getId()), any(String.class)))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void deletePost_UnauthorizedUser_Returns403() throws Exception {
        String errorMessage = "Cannot delete post of another user !";

        given(contentService.softDeleteContent(eq(testContent.getId()), any(String.class)))
                .willThrow(new UserActionForbiddenException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{postId}", testContent.getId())
                        .header("Authorization", VALID_HEADER))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void deletePost_WithoutAuthHeader_Returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{postId}", testContent.getId()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }
    //endregion

    //region<getPostById>
    @Test
    void getPostById_WithValidId_Returns200() throws Exception {
        given(contentService.getVisibleContentById(eq(testContent.getId())))
                .willReturn(testResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/{postId}", testContent.getId()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testContent.getId().toString()))
                .andExpect(jsonPath("$.text").value(testContent.getText()))
                .andExpect(jsonPath("$.media_urls.image").value(testContent.getMediaUrls().get("image")));
    }

    @Test
    void getPostById_NonExistentPost_Returns404() throws Exception {
        UUID nonExistentPostId = UUID.randomUUID();
        String errorMessage = "Cannot find post with userId " + nonExistentPostId;

        given(contentService.getVisibleContentById(eq(nonExistentPostId)))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/{postId}", nonExistentPostId))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void getPostById_DeletedPost_Returns404() throws Exception {
        String errorMessage = "Cannot find post with userId " + testContent.getId();

        given(contentService.getVisibleContentById(eq(testContent.getId())))
                .willThrow(new PostNotFoundException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/{postId}", testContent.getId()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(errorMessage)));
    }

    @Test
    void getPostById_WithNullId_Returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/ "))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }
    //endregion

    //region<getAllPostsFromUsername>
    @Test
    void getAllPostsFromUsername_WithValidRequest_Returns200() throws Exception {
        List<ContentResponse> contentList = List.of(testResponse);
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                contentList,
                0,
                1,
                contentList.size(),
                contentList.size()
        );

        given(contentService.getAllVisibleContentFromUser(eq("testuser"), any(Pageable.class), eq("all")))
                .willReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/testuser/posts")
                        .param("type", "all")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testContent.getId().toString()))
                .andExpect(jsonPath("$.content[0].text").value(testContent.getText()))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllPostsFromUsername_WithPostType_ReturnsOnlyPosts() throws Exception {
        List<ContentResponse> contentList = List.of(testResponse);
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                contentList,
                0,
                1,
                contentList.size(),
                contentList.size()
        );

        given(contentService.getAllVisibleContentFromUser(eq("testuser"), any(Pageable.class), eq("post")))
                .willReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/testuser/posts")
                        .param("type", "post")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void getAllPostsFromUsername_WithCommentType_ReturnsOnlyComments() throws Exception {
        List<ContentResponse> contentList = List.of(testResponse);
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                contentList,
                0,
                1,
                contentList.size(),
                contentList.size()
        );

        given(contentService.getAllVisibleContentFromUser(eq("testuser"), any(Pageable.class), eq("comment")))
                .willReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/testuser/posts")
                        .param("type", "comment")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void getAllPostsFromUsername_WithPagination_ReturnsCorrectPage() throws Exception {
        List<ContentResponse> contentList = Collections.emptyList();
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                contentList,
                1,
                2,
                20,
                10
        );

        given(contentService.getAllVisibleContentFromUser(eq("testuser"), any(Pageable.class), eq("all")))
                .willReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/testuser/posts")
                        .param("type", "all")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(20));
    }

    @Test
    void getAllPostsFromUsername_WithInvalidType_Returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/testuser/posts")
                        .param("type", "invalid")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPostsFromUsername_WithNonExistentUser_Returns404() throws Exception {
        String username = "nonexistent";
        String errorMessage = "User not found: " + username;

        given(contentService.getAllVisibleContentFromUser(eq(username), any(Pageable.class), anyString()))
                .willThrow(new UserDoesntExistsException(errorMessage));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile/{username}/posts", username)
                        .param("type", "all")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }
    //endregion

    //region<getUserFeed>

    //endregion
}