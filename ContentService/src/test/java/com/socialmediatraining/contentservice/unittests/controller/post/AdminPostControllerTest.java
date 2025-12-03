package com.socialmediatraining.contentservice.unittests.controller.post;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.service.post.ContentService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPostControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ContentService contentService;

    private final static String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5zb2NpY" +
            "WxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";
    private Content contentTest;
    private ExternalUser user;

    @BeforeEach
    void setup(){
        contentTest = Content.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .text("text")
                .mediaUrls(new HashMap<>())
                .deletedAt(LocalDateTime.now())
                .build();

        user = ExternalUser.builder()
                .id(UUID.randomUUID())
                .username("username")
                .build();
    }

    @Test
    void getAllPostsFromUsername_WithValidRequest_Returns200AndPageOfPosts() throws Exception {
        PageResponse<ContentResponseAdmin> mockResponse = new PageResponse<>(
                Collections.emptyList(), 0, 1, 0, 0
        );

        given(contentService.getAllContentFromUser(anyString(), any(Pageable.class),anyString())).willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "all")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalItems", is(0)));
    }

    @Test
    void getAllPostsFromUsername_WithInvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "invalid")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPostsFromUsername_WhenInvalidUsername_ReturnsUserDoesntExists() throws Exception {
        given(contentService.getAllContentFromUser(anyString(), any(Pageable.class), anyString()))
                .willThrow(new UserDoesntExistsException("User not found: " + user.getUsername()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "all"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPostsFromUsername_WithInvalidPageParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "all")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPostsFromUsername_WithPostType_ReturnsOnlyPosts() throws Exception {
        PageResponse<ContentResponseAdmin> mockResponse = new PageResponse<>(
                Collections.singletonList(ContentResponseAdmin.fromEntity(contentTest)),
                1, 1, 1, 1);

        given(contentService.getAllContentFromUser(anyString(), any(Pageable.class), eq("post")))
                .willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "post")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postResponse.id", is(contentTest.getId().toString())));
    }

    @Test
    void getAllPostsFromUsername_WithCommentType_ReturnsOnlyComments() throws Exception {
        PageResponse<ContentResponseAdmin> mockResponse = new PageResponse<>(
                Collections.singletonList(ContentResponseAdmin.fromEntity(contentTest)),
                1, 1, 1, 1);

        given(contentService.getAllContentFromUser(anyString(), any(Pageable.class), eq("comment")))
                .willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "comment")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postResponse.id", is(contentTest.getId().toString())));
    }

    @Test
    void getAllPostsFromUsername_WithPagination_ReturnsCorrectPage() throws Exception {
        PageResponse<ContentResponseAdmin> mockResponse = new PageResponse<>(
                Collections.emptyList(), 0, 2, 15, 10
        );

        given(contentService.getAllContentFromUser(anyString(), any(Pageable.class), anyString()))
                .willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/profile/" + user.getUsername())
                        .header("Authorization", VALID_HEADER)
                        .param("type", "all")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.totalItems", is(15)));
    }

    @Test
    void getPostById_WithValidId_Returns200AndPost() throws Exception {
        ContentResponseAdmin mockResponse = ContentResponseAdmin.fromEntity(contentTest);

        given(contentService.getContentByIdWithDeleted(any(UUID.class))).willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postResponse.id", is(contentTest.getId().toString())));
    }

    @Test
    void getPostById_WithNonExistentId_Returns404() throws Exception {
        given(contentService.getContentByIdWithDeleted(any(UUID.class)))
                .willThrow(new PostNotFoundException("Cannot find post with userId "+ contentTest.getId()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void getPostById_WithDeletedPost_ReturnsPost() throws Exception {
        ContentResponseAdmin mockResponse = ContentResponseAdmin.fromEntity(contentTest);
        given(contentService.getContentByIdWithDeleted(any(UUID.class))).willReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/admin/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postResponse.id", is(contentTest.getId().toString())))
                .andExpect(jsonPath("$.deletedAt").exists());
    }
}