package com.socialmediatraining.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(AuthenticationTestConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    @Autowired
    private AuthService authService;

    @Test
    void signUp_with_valid_user_returns_created() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "testuser",
                "test@test.com",
                "test1234",
                List.of("USER"),
                "fistName",
                "lastName",
                "2000-01-01",
                "description"
                );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        given(authService.signUp(any(UserSignUpRequest.class))).willReturn("User created successfully");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getContentAsString()).isEqualTo("User created successfully");
        verify(authService).signUp(any(UserSignUpRequest.class));
    }

    @Test
    void signUp_with_invalid_email_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "testUser",
                "invalid-email",
                "test1234",
                List.of("USER"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_invalid_username_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "",
                "user@user.com",
                "test1234",
                List.of("USER"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_invalid_password_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "",
                List.of("USER"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_semi_invalid_email_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@.com",
                "test",
                List.of("USER"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_empty_roles_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of(),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_invalid_roles_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("INVALID"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_several_invalid_values_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "test",
                "test",
                List.of("INVALID"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_duplicated_roles_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER","USER"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void signUp_with_wrong_case_roles_returns_created() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("user","ADMIN"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

         mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(request.roles().contains("USER")).isTrue();
    }

    @Test
    void signUp_with_multiple_roles_returns_created() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER","ADMIN"),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void signUp_with_whitespace_roles_returns_bad_request() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER "),
                "fistName",
                "lastName",
                "description",
                "2000-01-01"

        );
        String jsonRequest = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }
}

@TestConfiguration
class AuthenticationTestConfig {
    @Bean
    @Primary
    public AuthService authService() {
        return mock(AuthService.class);
    }
}