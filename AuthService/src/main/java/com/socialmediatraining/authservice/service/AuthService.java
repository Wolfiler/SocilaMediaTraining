package com.socialmediatraining.authservice.service;

import com.socialmediatraining.authenticationcommons.dto.SimpleUserDataObject;
import com.socialmediatraining.authservice.dto.UserResponse;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.dto.UserUpdateRequest;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import com.socialmediatraining.exceptioncommons.exception.AuthUserCreationException;
import com.socialmediatraining.exceptioncommons.exception.InvalidAuthorizationHeaderException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;

@Service
@Slf4j
public class AuthService {
    private final Keycloak keycloak;
    private final KeycloakPropertiesUtils keycloakProperties;
    private final KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate;

    @Autowired
    public AuthService(Keycloak keycloak, KeycloakPropertiesUtils keycloakProperties, KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate) {
        this.keycloak = keycloak;
        this.keycloakProperties = keycloakProperties;
        this.userDataKafkaTemplate = userDataKafkaTemplate;
    }

    public UserRepresentation getUserRepresentation(UserSignUpRequest signUpRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(signUpRequest.username() + ".socialmedia");
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

    private Map<String, List<String>> createUserAttributesMap(
            String firstName, String lastName, String dateOfBirth,String description,String profilePicture){
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("firstName", Collections.singletonList(firstName));
        attributes.put("lastName",Collections.singletonList(lastName));
        attributes.put("dateOfBirth",Collections.singletonList(dateOfBirth));
        attributes.put("description",Collections.singletonList(description));
        attributes.put("profilePicture",Collections.singletonList(profilePicture));
        return attributes;
    }

    private void checkUserBirthDateValidity(String dateOfBirth){
        if(dateOfBirth == null || !dateOfBirth.matches("\\d{4}-[01]\\d-[0-3]\\d")){
            throw new AuthUserCreationException("Attribute dateOfBirth is not using the correct yyyy-MM-dd format: " + dateOfBirth);
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(false);
        try {
            df.parse(dateOfBirth);
        } catch (ParseException ex) {
            throw new AuthUserCreationException("Attribute dateOfBirth is using an impossible date (month > 12 or day > 31): " + dateOfBirth);
        }
    }

    private void checkUserEmailValidity(String email){
        if(email == null || !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")){
            throw new AuthUserCreationException("Attribute email is not using the correct format: " + email);
        }
    }

    public String signUp(UserSignUpRequest signUpRequest) throws AuthUserCreationException {
        try {
            checkUserBirthDateValidity(signUpRequest.dateOfBirth());
            checkUserEmailValidity(signUpRequest.email());
            UserRepresentation user = getUserRepresentation(signUpRequest);
            user.setAttributes(createUserAttributesMap(
                    signUpRequest.firstName(),
                    signUpRequest.lastName(),
                    signUpRequest.dateOfBirth(),
                    signUpRequest.description(),
                    "NewUser.png"
                    ));

            try (Response response = keycloak.realm(keycloakProperties.realm).users().create(user)) {
                if (response.getStatus() == 201) {
                    //TODO Kafka topic on new user in db

                    String userId = CreatedResponseUtil.getCreatedId(response);
                    SimpleUserDataObject userData = new SimpleUserDataObject(userId, user.getUsername());

                    if(userData.username() == null){//Might not be useful check, maybe remove later
                        log.error("ERROR: impossible to get user id from db - other db will not be updated!");
                    }else{
                        userDataKafkaTemplate.send("created-new-user", userData);
                    }
                    return "User successfully created";
                } else {
                    throw new AuthUserCreationException("Failed to create user: " + response.readEntity(String.class));
                }
            }
        } catch (Exception e) {
            throw new AuthUserCreationException("Error during user creation process: " + e.getMessage());
        }
    }

    public String logout(String authHeader) throws InvalidAuthorizationHeaderException {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String subId = getSubIdFromAuthHeader(authHeader);
            try {
                keycloak.realm(keycloakProperties.realm).users().get(subId).logout();
                return "User logged out";
            } catch (Exception e) {
                throw new RuntimeException("Error during logout: " + e.getMessage());
            }
        }
        throw new InvalidAuthorizationHeaderException("ERROR: Invalid authorization header");
    }

    public UserResponse getUserInformation(String authHeader) {
        String subId = getSubIdFromAuthHeader(authHeader);
        UserRepresentation response = keycloak.realm(keycloakProperties.realm).users().get(subId).toRepresentation();

        return new UserResponse(
                response.getId(),
                response.getUsername(),
                response.getFirstName(),
                response.getLastName(),
                response.getEmail(),
                response.getAttributes().get("dateOfBirth").getFirst(),
                response.getAttributes().get("description").getFirst(),
                response.getAttributes().get("profilePicture").getFirst()
        );
    }

    public UserResponse updateUserInformation(String authHeader, UserUpdateRequest updatedRequest) throws UserDoesntExistsException,ClientErrorException {
        checkUserEmailValidity(updatedRequest.email());
        checkUserBirthDateValidity(updatedRequest.dateOfBirth());

        String subId = getSubIdFromAuthHeader(authHeader);
        UserResource userResource = keycloak.realm(keycloakProperties.realm).users().get(subId);
        if(userResource == null){
            throw new UserDoesntExistsException(String.format("User id %s not found in db",subId));
        }

        UserRepresentation userRep = userResource.toRepresentation();
        userRep.setFirstName(updatedRequest.firstName());
        userRep.setLastName(updatedRequest.lastName());
        userRep.setEmail(updatedRequest.email());
        userRep.setAttributes(createUserAttributesMap(updatedRequest.firstName(),updatedRequest.lastName(),
                updatedRequest.dateOfBirth(),updatedRequest.description(),updatedRequest.profilePicture()));

        keycloak.realm(keycloakProperties.realm).users().get(subId).update(userRep);

        //TODO Kafka topic on username update

        return new UserResponse(
                userRep.getId(),
                userRep.getUsername(),
                userRep.getFirstName(),
                userRep.getLastName(),
                userRep.getEmail(),
                userRep.getAttributes().get("dateOfBirth").getFirst(),
                userRep.getAttributes().get("description").getFirst(),
                userRep.getAttributes().get("profilePicture").getFirst()
        );
    }

    public String deleteUser(String authHeader){
        String subId = getSubIdFromAuthHeader(authHeader);
        keycloak.realm(keycloakProperties.realm).users().get(subId).remove();
        return "User deleted successfully";
    }
}
