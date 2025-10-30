package com.socialmediatraining.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.handler.CustomLogoutHandler;
import com.socialmediatraining.authservice.service.AuthService;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(AuthenticationTestConfig.class)
class SecurityConfigTest {
    @Mock
    private CustomLogoutHandler customLogoutHandler;
    @Mock
    private KeycloakPropertiesUtils keycloakProperties;
    @MockitoBean
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    MockMvc mockMvc;

    @Test
    void testJwtDecoderBean() {
        SecurityConfig securityConfig = new SecurityConfig(customLogoutHandler, keycloakProperties);
        String testJwkUri = "http://test-uri";
        given(keycloakProperties.getJwkSetUri()).willReturn(testJwkUri);

        JwtDecoder jwtDecoder = securityConfig.jwtDecoder();

        assertThat(jwtDecoder).isNotNull();
        assertThat(keycloakProperties.getJwkSetUri()).isEqualTo("http://test-uri");
    }

    @Test
    void test_visit_signIn_as_unauthorized_should_be_ok() throws Exception {
        given(keycloakProperties.getAuthServerUrl()).willReturn("http://localhost:8100");
        given(keycloakProperties.getRealm()).willReturn("social-media-training");
        given(keycloakProperties.getClientId()).willReturn("social-media-user-id");
        given(keycloakProperties.getClientSecret()).willReturn("***secret***");

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/signin"))
                .andExpect(status().isOk());
    }

    @Test
    void test_visit_signUp_as_unauthorized_should_be_created() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest(
                "test",
                "test@test.com",
                "test1234",
                new ArrayList<>(List.of("USER"))
        ) ;

        String jsonRequest = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void test_visit_admin_welcome_as_unauthorized_should_be_unauthorized() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/admin/welcome"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_visit_admin_welcome_as_admin_should_be_ok() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/admin/welcome"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void test_visit_admin_welcome_as_user_should_be_forbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/admin/welcome"))
                .andExpect(status().isForbidden());
    }

    @Test
    void test_visit_user_welcome_as_unauthorized_should_not_be_ok() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/welcome"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void test_visit_user_welcome_as_user_should_be_ok() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/welcome"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void test_visit_user_welcome_as_admin_should_be_ok() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/auth/welcome"))
                .andExpect(status().isOk());
    }

    @Test
    void valid_Jwt_should_authenticate_user() throws Exception {
        // Mock JWT decoder to return a valid token
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "read")
                .claim("realm_access", Map.of("roles",
                        List.of("ROLE_USER")))
                .build();

        given(jwtDecoder.decode(anyString())).willReturn(jwt);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/welcome")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void should_be_stateless() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("scope", "read")
                .claim("realm_access", Map.of("roles",
                        List.of("ROLE_USER")))
                .build();

        given(jwtDecoder.decode(anyString())).willReturn(jwt);

        MvcResult result1 = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/welcome")
                        .header("Authorization", "Bearer token"))
                .andReturn();

        MvcResult result2 = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/welcome")
                        .header("Authorization", "Bearer token"))
                .andReturn();

        assertThat(result1.getRequest().getSession(false)).isNull();
        assertThat(result2.getRequest().getSession(false)).isNull();
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
