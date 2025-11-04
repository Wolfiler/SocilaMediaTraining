package com.socialmediatraining.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmediatraining.authservice.dto.UserResponse;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import com.socialmediatraining.exceptioncommons.exception.InvalidAuthorizationHeaderException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class AuthService {
    private final Keycloak keycloak;
    private final WebClient.Builder webClient;
    private final KeycloakPropertiesUtils keycloakProperties;

    @Autowired
    public AuthService(Keycloak keycloak, WebClient.Builder webclient, KeycloakPropertiesUtils keycloakProperties) {
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

    private Map<String, List<String>> createUserAttributesMap(UserSignUpRequest signUpRequest){
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("firstName", Collections.singletonList(signUpRequest.firstName()));
        attributes.put("lastName",Collections.singletonList(signUpRequest.lastName()));
        attributes.put("dateOfBirth",Collections.singletonList(signUpRequest.dateOfBirth()));
        attributes.put("description",Collections.singletonList(signUpRequest.description()));
        attributes.put("profilePicture",Collections.singletonList("NewUser.png"));
        return attributes;
    }

    //Can be expanded upon for email for example
    private void checkUserAttributesValidity(UserSignUpRequest signUpRequest){
        String dateOfBirthString = signUpRequest.dateOfBirth();
        if(signUpRequest.dateOfBirth() == null || !dateOfBirthString.matches("\\d{4}-[01]\\d-[0-3]\\d")){
            throw new AuthUserCreationException("Attribute dateOfBirth is not using the correct yyyy-MM-dd format: " + dateOfBirthString);
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(false);
        try {
            df.parse(dateOfBirthString);
        } catch (ParseException ex) {
            throw new AuthUserCreationException("Attribute dateOfBirth is using an impossible date e.g. 2025-13-12");
        }

        String email = signUpRequest.email();
        if(email == null || !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")){
            throw new AuthUserCreationException("Attribute email is not using the correct format: " + email);
        }
    }

    public String signUp(UserSignUpRequest signUpRequest) throws AuthUserCreationException {
        try {
            checkUserAttributesValidity(signUpRequest);
            UserRepresentation user = getUserRepresentation(signUpRequest);
            user.setAttributes(createUserAttributesMap(signUpRequest));

            try (Response response = keycloak.realm(keycloakProperties.realm).users().create(user)) {
                if (response.getStatus() == 201) {
                    return "User successfully created";
                } else {
                    throw new AuthUserCreationException("Failed to create user: " + response.readEntity(String.class));
                }
            }
        } catch (Exception e) {
            throw new AuthUserCreationException("Error during user creation process: " + e.getMessage());
        }
    }

    public String logout(String authHeader,String refreshToken) throws InvalidAuthorizationHeaderException {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", keycloakProperties.getAuthServerUrl(), keycloakProperties.getRealm());
            try {
                String clientResponse = webClient.build().post()
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

    public UserResponse getUserInformation(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        JsonNode jsonNode = null;

        try {
            jsonNode = new ObjectMapper().readTree(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage());
        }
        String subId = jsonNode.get("sub").asText();
        UserRepresentation response = keycloak.realm(keycloakProperties.realm).users().get(subId).toRepresentation();

        Date date = new Date();
        date.setTime(response.getCreatedTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        return new UserResponse(
                response.getId(),
                response.getFirstName(),
                response.getLastName(),
                response.getEmail(),
                sdf.format(date),
                response.getAttributes().get("dateOfBirth").getFirst(),
                response.getAttributes().get("description").getFirst(),
                response.getAttributes().get("profilePicture").getFirst()
        );
    }
}
