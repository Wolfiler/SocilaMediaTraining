package com.socialmediatraining.contentservice.unittests.controller.like;

import com.socialmediatraining.contentservice.controller.like.LikeController;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.service.like.LikeService;
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

import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Collections;
import java.util.List;


@WebMvcTest(LikeController.class)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private LikeService likeService;

    private Content contentTest;

    private ExternalUser user;

    private final static String VALID_HEADER = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlci5zb2NpY" +
            "WxtZWRpYSJ9.l3I61dPRuMr3zqXoG3TqOAJ0_4tnf3z_LcU4TtUM4fw";

    @BeforeEach
    void setup(){
        contentTest = Content.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .text("text")
                .mediaUrls(new HashMap<>())
                .build();

        user = ExternalUser.builder()
                .id(UUID.randomUUID())
                .username("username")
                .build();
    }

    @Test
    void likePost_WhenValidRequest_ReturnsCreated() throws Exception {
        String expectedResponse = "User " + user.getId() + " liked post with userId " + contentTest.getId();

        given(likeService.likeContent(anyString(),any(UUID.class))).willReturn(expectedResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(expectedResponse)));
    }

    @Test
    void likePost_WhenInvalidPostId_ReturnsPostNotFound() throws Exception {
        String expectedResponse = "Post with userId " + contentTest.getId() + " doesn't exists";

        given(likeService.likeContent(anyString(),any(UUID.class)))
                .willThrow(new PostNotFoundException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"404 NOT_FOUND")));
    }

    @Test
    void likePost_WhenMissingAuthHeader_ReturnsUnauthorized() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/like/" + contentTest.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likePost_WhenInvalidUser_ReturnsUserNotFound() throws Exception{
        String expectedResponse = "User not found: " + user.getUsername();

        given(likeService.likeContent(anyString(),any(UUID.class)))
                .willThrow(new UserDoesntExistsException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"404 NOT_FOUND")));
    }

    @Test
    void likePost_WhenPostAlreadyLiked_ReturnsUserActionForbidden() throws Exception{
        String expectedResponse = "User " + user.getUsername() + " already liked post with id " + contentTest.getId();

        given(likeService.likeContent(anyString(),any(UUID.class)))
                .willThrow(new UserActionForbiddenException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"403 FORBIDDEN")));
    }

    @Test
    void getAllLikedPosts_WithPagination_Returns200AndPageOfLikedContents() throws Exception {
        PageResponse<ContentResponse> pageResponse = new PageResponse<>(
                Collections.singletonList(ContentResponse.fromEntity(contentTest)),
                0,
                1,
                1,
                1);

        given(likeService.getAllLikedContentsByUser(anyString(), any(Pageable.class)))
                .willReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/like/profile/" + user.getId())
                        .header("Authorization", VALID_HEADER)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(contentTest.getId().toString())));
    }

    @Test
    void getAllLikedPosts_WhenInvalidUsername_ReturnsUserDoesntExists() throws Exception {
        String expectedResponse = "User not found with id: " + user.getId();

        given(likeService.getAllLikedContentsByUser(anyString(), any(Pageable.class)))
                .willThrow(new UserDoesntExistsException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/like/profile/" + user.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"404 NOT_FOUND")));
    }

    @Test
    void getAllLikedPosts_WhenInvalidPagination_ReturnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/like/profile/" + user.getId())
                        .header("Authorization", VALID_HEADER)
                        .param("page", "-1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/like/profile/" + user.getId())
                        .header("Authorization", VALID_HEADER)
                        .param("page", "0")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllLikedPosts_WhenNoLikedPosts_ReturnsEmptyPage() throws Exception {
        given(likeService.getAllLikedContentsByUser(anyString(), any(Pageable.class)))
                .willReturn(new PageResponse<>(Collections.emptyList(), 0, 0, 0, 0));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/like/profile/" + user.getId())
                        .header("Authorization", VALID_HEADER)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalItems", is(0)));
    }

    @Test
    void deleteLike_WhenValidRequest_ReturnsOk() throws Exception {
        String expectedResponse = "User " + user.getId() + " unliked post with id " + contentTest.getId();

        given(likeService.deleteLike(anyString(), any(UUID.class))).willReturn(expectedResponse);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedResponse)));
    }


    @Test
    void deleteLike_WhenInvalidUserId_ReturnsUserDoesntExists() throws Exception {
        String expectedResponse = "User not found: " + user.getUsername();

        given(likeService.deleteLike(anyString(), any(UUID.class)))
                .willThrow(new UserDoesntExistsException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"404 NOT_FOUND")));
    }


    @Test
    void deleteLike_WhenInvalidPostId_ReturnsPostNotFound() throws Exception {
        String expectedResponse = "Post with id " + contentTest.getId() + " doesn't exist";

        given(likeService.deleteLike(anyString(), any(UUID.class)))
                .willThrow(new PostNotFoundException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"404 NOT_FOUND")));
    }

    @Test
    void deleteLike_WhenPostIsNotLiked_ReturnsUserActionForbidden() throws Exception {
        String expectedResponse = "User " + user.getUsername() + " has not liked post with id " + contentTest.getId();

        given(likeService.deleteLike(anyString(), any(UUID.class)))
                .willThrow(new UserActionForbiddenException(expectedResponse));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/like/" + contentTest.getId())
                        .header("Authorization", VALID_HEADER)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString(expectedResponse)))
                .andExpect(content().string(containsString("HttpStatus: \":\"403 FORBIDDEN")));
    }


    @Test
    void deleteLike_WhenMissingAuthHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/like/" + contentTest.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }
}
