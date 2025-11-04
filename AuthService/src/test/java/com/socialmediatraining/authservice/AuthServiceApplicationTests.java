package com.socialmediatraining.authservice;

import com.socialmediatraining.authservice.dto.UserResponse;
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
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceApplicationTests {
    @Mock
    private Keycloak keycloak;
    @Mock
    private WebClient.Builder webClient;
    @Mock
    private KeycloakPropertiesUtils keycloakProperties;
    @Mock
    private UsersResource usersResource;
    @Mock
    private UserResource userResource;
    @Mock
    private UserRepresentation userRepresentation;
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
                List.of("USER"),
                "fistName",
                "lastName",
                "2000-01-01",
                "description"
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

        assertThat(result).isEqualTo("User successfully created");
        verify(usersResource).create(any(UserRepresentation.class));
    }

    @Test
    void signUp_should_return_user_date_of_birth_format_error() {
        userSignUpRequest = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER"),
                "fistName",
                "lastName",
                "12-12-2000",
                "description"
        );

        Exception exception = assertThrows(AuthUserCreationException.class, () -> {
            authService.signUp(userSignUpRequest);
        });

        assertThat(exception.getMessage()).contains("Attribute dateOfBirth is not using the correct yyyy-MM-dd format");
    }

    @Test
    void signUp_should_return_user_date_of_birth_format_error_with_impossible_date() {
        userSignUpRequest = new UserSignUpRequest(
                "test",
                "user@user.com",
                "test",
                List.of("USER"),
                "fistName",
                "lastName",
                "2000-15-01",
                "description"
        );

        Exception exception = assertThrows(AuthUserCreationException.class, () -> {
            authService.signUp(userSignUpRequest);
        });

        assertThat(exception.getMessage()).contains("Attribute dateOfBirth is using an impossible date");
    }

    @Test
    void signUp_should_return_wrong_email_format() {
        userSignUpRequest = new UserSignUpRequest(
                "test",
                "user.user.com",
                "test",
                List.of("USER"),
                "fistName",
                "lastName",
                "2000-01-01",
                "description"
        );

        UserSignUpRequest userSignUpRequest2 = new UserSignUpRequest(
                "test",
                "user@userdotcom",
                "test",
                List.of("USER"),
                "fistName",
                "lastName",
                "2000-01-01",
                "description"
        );

        Exception exception1 = assertThrows(AuthUserCreationException.class, () -> {
            authService.signUp(userSignUpRequest);
        });

        Exception exception2 = assertThrows(AuthUserCreationException.class, () -> {
            authService.signUp(userSignUpRequest2);
        });

        assertThat(exception1.getMessage()).contains("Attribute email is not using the correct format");
        assertThat(exception2.getMessage()).contains("Attribute email is not using the correct format");
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
        boolean authUserCreationExceptionThrown = false;

        String errorMessage = "";

        try{
            authService.signUp(userSignUpRequest);
        }catch (AuthUserCreationException e){
            authUserCreationExceptionThrown = true;
            errorMessage = e.getMessage();
        }

        assertThat(errorMessage).contains("Error during user creation process");
        assertThat(authUserCreationExceptionThrown).isTrue();
    }

    @Test
    void signUp_should_throw_AuthUserCreationException_if_user_resources_not_available() {
        boolean authUserCreationExceptionThrown = false;

        String errorMessage = "";

        try{
            authService.signUp(userSignUpRequest);
        }catch (AuthUserCreationException e){
            authUserCreationExceptionThrown = true;
            errorMessage = e.getMessage();
        }

        assertThat(errorMessage).contains("Error during user creation process");
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

        WebClient.Builder testWebClient = WebClient.builder().baseUrl(mockServerUrl);

        AuthService testAuthService = new AuthService(keycloak, testWebClient, keycloakProperties);
        String response = testAuthService.logout("Bearer token", "refreshtoken");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(response).isEqualTo("User logged out");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).contains("/realms/test-realm/protocol/openid-connect/logout");
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    @Test
    void get_authenticated_user_should_return_user_data(){
        given(keycloak.realm(any())).willReturn(realmResource);
        given(keycloak.realm(any()).users()).willReturn(usersResource);
        given(usersResource.get(anyString())).willReturn(userResource);
        given(userResource.toRepresentation()).willReturn(userRepresentation);

        given(userRepresentation.getId()).willReturn("valid-id-for-user");
        given(userRepresentation.getFirstName()).willReturn("firstName");
        given(userRepresentation.getLastName()).willReturn("lastName");
        given(userRepresentation.getEmail()).willReturn("email");
        given(userRepresentation.getCreatedTimestamp()).willReturn(123456789L);

        Date date = new Date();
        date.setTime(userRepresentation.getCreatedTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        given(userRepresentation.getAttributes()).willReturn(new HashMap<>(){
            {
                put("dateOfBirth", List.of("2000-01-01"));
                put("description", List.of("description"));
                put("profilePicture", List.of("profilePicture.png"));
            }
        });

        UserResponse userResponse = authService.getUserInformation("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6" +
                "IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTA" +
                "yMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30");

        assertThat(userResponse.id()).isEqualTo("valid-id-for-user");
        assertThat(userResponse.firstName()).isEqualTo("firstName");
        assertThat(userResponse.lastName()).isEqualTo("lastName");
        assertThat(userResponse.email()).isEqualTo("email");
        assertThat(userResponse.dateOfBirth()).isEqualTo("2000-01-01");
        assertThat(userResponse.creationDate()).isEqualTo(sdf.format(date));
        assertThat(userResponse.description()).isEqualTo("description");
        assertThat(userResponse.profilePicture()).isEqualTo("profilePicture.png");
    }

    @Test
    void get_authenticated_user_should_throw_runtimeException_with_invalid_token(){
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.getUserInformation("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiezaeez" +
                    "aeaeqqIxMjM0NTYf3ODkwIiwibmFtZSI6IkpvfaG4gRG9lIifwiYWRtaW4iOnRydWUsImlhdfCI6MTUxNjIzOTAyMn0.K" +
                    "MUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30");
        });

        assertThat(exception.getMessage()).contains("Error processing JSON");
    }
}