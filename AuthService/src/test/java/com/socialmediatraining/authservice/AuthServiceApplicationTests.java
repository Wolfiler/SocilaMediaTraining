package com.socialmediatraining.authservice;

import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.service.AuthService;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceApplicationTests {
    @Mock
    private Keycloak keycloak;
    @Mock
    private WebClient webClient;
    @Mock
    private KeycloakPropertiesUtils keycloakProperties;
    @Mock
    private UsersResource usersResource;
    @Mock
    private RealmResource realmResource;
    @Mock
    private Response response;
    @InjectMocks
    private AuthService authService;
    private UserSignUpRequest userSignUpRequest;

    @BeforeEach
    void setup(){
        userSignUpRequest = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER")
        );
    }

    @Test
    void getUserRepresentation_should_create_user_with_correct_information(){

        AuthService service = new AuthService(keycloak, webClient, keycloakProperties);

        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(userSignUpRequest.password());

        UserRepresentation userRepresentation = service.getUserRepresentation(userSignUpRequest);
        assertThat(userRepresentation.getUsername()).isEqualTo(userSignUpRequest.username());
        assertThat(userRepresentation.getEmail()).isEqualTo(userSignUpRequest.email());
        assertThat(userRepresentation.getGroups().containsAll(userSignUpRequest.roles())).isTrue();
        assertThat(userSignUpRequest.roles().containsAll(userRepresentation.getGroups())).isTrue();
        assertThat(userRepresentation.getCredentials().getFirst().getValue()).isEqualTo(credentials.getValue());
    }

    @Test
    void signUp_should_return_user_created_successfully() {
        given(keycloak.realm(any())).willReturn(realmResource);
        given(realmResource.users()).willReturn(usersResource);
        given(usersResource.create(any(UserRepresentation.class))).willReturn(response);
        given(response.getStatus()).willReturn(201);

        String result = authService.signUp(userSignUpRequest);

        assertThat(result).isEqualTo("User created successfully");
        verify(usersResource).create(any(UserRepresentation.class));
    }

    @Test
    void signUp_should_throw_AuthUserCreationException_if_return_code_not_201() {
        given(keycloak.realm(any())).willReturn(realmResource);
        given(realmResource.users()).willReturn(usersResource);
        given(usersResource.create(any(UserRepresentation.class))).willReturn(response);
        given(response.getStatus()).willReturn(400);
        boolean authUserCreationExceptionThrown = false;

        String errorMessage = "";

        try{
            authService.signUp(userSignUpRequest);
        }catch (AuthUserCreationException e){
            authUserCreationExceptionThrown = true;
            errorMessage = e.getMessage();
        }

        assertThat(errorMessage).contains("Failed to create user:");
        assertThat(authUserCreationExceptionThrown).isTrue();
    }

    @Test
    void signUp_should_throw_AuthUserCreationException_if_realm_not_available() {
        given(keycloak.realm(any())).willReturn(null);
        boolean authUserCreationExceptionThrown = false;

        String errorMessage = "";

        try{
            authService.signUp(userSignUpRequest);
        }catch (AuthUserCreationException e){
            authUserCreationExceptionThrown = true;
            errorMessage = e.getMessage();
        }

        assertThat(errorMessage).contains("Error creating user, the realm might be unavailable");
        assertThat(authUserCreationExceptionThrown).isTrue();
    }

    @Test
    void signUp_should_throw_AuthUserCreationException_if_user_resources_not_available() {
        given(keycloak.realm(any())).willReturn(realmResource);
        given(realmResource.users()).willReturn(null);

        boolean authUserCreationExceptionThrown = false;

        String errorMessage = "";

        try{
            authService.signUp(userSignUpRequest);
        }catch (AuthUserCreationException e){
            authUserCreationExceptionThrown = true;
            errorMessage = e.getMessage();
        }

        assertThat(errorMessage).contains("Error creating user, the realm might be unavailable");
        assertThat(authUserCreationExceptionThrown).isTrue();
    }

    private final MockWebServer mockWebServer = new MockWebServer();

    @Test
    void logout_should_work_when_given_authorization_and_refresh_token() throws InterruptedException {
        String mockServerUrl = mockWebServer.url("/").toString();
        given(keycloakProperties.getAuthServerUrl()).willReturn(mockServerUrl);
        given(keycloakProperties.getRealm()).willReturn("test-realm");
        given(keycloakProperties.getClientId()).willReturn("test-client");
        given(keycloakProperties.getClientSecret()).willReturn("test-secret");

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody("User logged out")
        );

        WebClient testWebClient = WebClient.builder()
                .baseUrl(mockServerUrl)
                .build();

        AuthService testAuthService = new AuthService(keycloak, testWebClient, keycloakProperties);
        String response = testAuthService.logout("Bearer token", "refreshtoken");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(response).isEqualTo("User logged out");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).contains("/realms/test-realm/protocol/openid-connect/logout");
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }
}