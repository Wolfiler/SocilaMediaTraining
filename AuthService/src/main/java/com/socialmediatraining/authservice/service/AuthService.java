package com.socialmediatraining.authservice.service;

import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import com.socialmediatraining.exceptioncommons.exception.InvalidAuthorizationHeaderException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.util.Collections;

@Service
@Slf4j
public class AuthService {
    private final Keycloak keycloak;
    private final WebClient webClient;
    private final KeycloakPropertiesUtils keycloakProperties;

    @Autowired
    public AuthService(Keycloak keycloak, WebClient webclient, KeycloakPropertiesUtils keycloakProperties) {
        this.keycloak = keycloak;
        this.webClient = webclient;
        this.keycloakProperties = keycloakProperties;
    }

    public UserRepresentation getUserRepresentation(UserSignUpRequest signUpRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(signUpRequest.username());
        user.setEmail(signUpRequest.email());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setGroups(signUpRequest.roles());

        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(signUpRequest.password());
        credentials.setTemporary(false);

        user.setCredentials(Collections.singletonList(credentials));
        return user;
    }

    //TODO: missing user already exists handling
    public String signUp(UserSignUpRequest signUpRequest) throws AuthUserCreationException {
        try {
            UserRepresentation user = getUserRepresentation(signUpRequest);

            try (Response response = keycloak.realm(keycloakProperties.realm).users().create(user)) {
                if (response.getStatus() == 201) {
                    return "User created successfully";
                } else {
                    throw new AuthUserCreationException("Failed to create user: " + response.readEntity(String.class));
                }
            }
        } catch (Exception e) {
            throw new AuthUserCreationException("Error creating user, the realm might be unavailable. Error message: " + e.getMessage());
        }
    }

    public String logout(String authHeader,String refreshToken) throws InvalidAuthorizationHeaderException {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", keycloakProperties.getAuthServerUrl(), keycloakProperties.getRealm());
            try {
                String clientResponse = webClient.post()
                        .uri(logoutUrl, UriBuilder::build)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", authHeader)
                        .body(BodyInserters
                                .fromFormData("client_id", keycloakProperties.getClientId())
                                .with("client_secret", keycloakProperties.getClientSecret())
                                .with("refresh_token", refreshToken)
                        )
                        .retrieve()
                        .onStatus(HttpStatus.BAD_REQUEST::equals, response -> response.bodyToMono(String.class).map(Exception::new))
                        .onStatus(HttpStatus.NOT_FOUND::equals, response -> response.bodyToMono(String.class).map(Exception::new))
                        .bodyToMono(String.class)
                        .block();
                return clientResponse != null ? clientResponse : "User logged out";
            } catch (Exception e) {
                throw new RuntimeException("Error during logout: " + e.getMessage());
            }
        }
        throw new InvalidAuthorizationHeaderException("ERROR: Invalid authorization header");
    }
}
