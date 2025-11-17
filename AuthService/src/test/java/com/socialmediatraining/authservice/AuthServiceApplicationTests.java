package com.socialmediatraining.authservice;

import com.socialmediatraining.authservice.dto.UserResponse;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.dto.UserUpdateRequest;
import com.socialmediatraining.authservice.service.AuthService;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import jakarta.ws.rs.core.Response;
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

    private final String correctHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6" +
            "IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTA" +
            "yMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

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

        AuthService service = new AuthService(keycloak, keycloakProperties);

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

    @Test
    void logout_should_work_when_given_authorization_header() {
        given(keycloak.realm(any())).willReturn(realmResource);
        given(keycloak.realm(any()).users()).willReturn(usersResource);
        given(usersResource.get(anyString())).willReturn(userResource);

        String response = authService.logout(correctHeader);

        assertThat(response).isEqualTo("User logged out");
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

        /*Date date = new Date();
        date.setTime(userRepresentation.getCreatedTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");*/

        given(userRepresentation.getAttributes()).willReturn(new HashMap<>(){
            {
                put("dateOfBirth", List.of("2000-01-01"));
                put("description", List.of("description"));
                put("profilePicture", List.of("profilePicture.png"));
            }
        });

        UserResponse userResponse = authService.getUserInformation(correctHeader);

        assertThat(userResponse.id()).isEqualTo("valid-id-for-user");
        assertThat(userResponse.firstName()).isEqualTo("firstName");
        assertThat(userResponse.lastName()).isEqualTo("lastName");
        assertThat(userResponse.email()).isEqualTo("email");
        assertThat(userResponse.dateOfBirth()).isEqualTo("2000-01-01");
        //assertThat(userResponse.creationDate()).isEqualTo(sdf.format(date));
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

    @Test
    void update_user_info_should_work_with_correct_body(){
        given(keycloak.realm(any())).willReturn(realmResource);
        given(keycloak.realm(any()).users()).willReturn(usersResource);
        given(usersResource.get(anyString())).willReturn(userResource);
        given(userResource.toRepresentation()).willReturn(userRepresentation);

        given(userRepresentation.getId()).willReturn("IdNumber");
        given(userRepresentation.getFirstName()).willReturn("firstName");
        given(userRepresentation.getLastName()).willReturn("lastName");
        given(userRepresentation.getEmail()).willReturn("email");
        given(userRepresentation.getAttributes()).willReturn(new HashMap<>(){
            {
                put("dateOfBirth", List.of("2000-01-01"));
                put("description", List.of("description"));
                put("profilePicture", List.of("profilePicture.png"));
            }
        });

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "username",
                "user@email.com",
                "firstName",
                "lastName",
                "2000-01-01",
                "description",
                "profilePicture.png"
        );

        UserResponse response = authService.updateUserInformation(correctHeader,updateRequest);

        assertThat(response.id()).isEqualTo("IdNumber");
        assertThat(response.firstName()).isEqualTo("firstName");
        assertThat(response.lastName()).isEqualTo("lastName");
        assertThat(response.email()).isEqualTo("email");
        assertThat(response.dateOfBirth()).isEqualTo("2000-01-01");
        assertThat(response.description()).isEqualTo("description");
        assertThat(response.profilePicture()).isEqualTo("profilePicture.png");
    }

    @Test
    void update_user_info_should_throw_UserDoesntExistsException_with_unknown_user(){
        given(keycloak.realm(any())).willReturn(realmResource);
        given(keycloak.realm(any()).users()).willReturn(usersResource);
        given(usersResource.get(anyString())).willReturn(null);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "username",
                "user@test.com",
                "firstName",
                "lastName",
                "2000-01-01",
                "description",
                "profilePicture.png"
        );

        Exception exception = assertThrows(UserDoesntExistsException.class, () -> {
            authService.updateUserInformation(correctHeader,updateRequest);
        });

        assertThat(exception).isInstanceOf(UserDoesntExistsException.class);
        assertThat(exception.getMessage().contains("User id 1234567890 not found in db")).isTrue();
    }

    @Test
    void delete_user_should_work_with_valid_user(){
        given(keycloak.realm(any())).willReturn(realmResource);
        given(keycloak.realm(any()).users()).willReturn(usersResource);
        given(usersResource.get(anyString())).willReturn(userResource);

        String response = authService.deleteUser(correctHeader);

        assertThat(response).isEqualTo("User deleted successfully");
    }

    @Test
    void delete_user_should_not_work_with_invalid_user_id(){
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.deleteUser("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiezaeez" +
                    "aeaeqqIxMjM0NTYf3ODkwIiwibmFtZSI6IkpvfaG4gRG9lIifwiYWRtaW4iOnRydWUsImlhdfCI6MTUxNjIzOTAyMn0.K" +
                    "MUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30");
        });

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage().contains("Error processing JSON")).isTrue();
    }
}