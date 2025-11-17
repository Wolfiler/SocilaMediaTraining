package com.socialmediatraining.authenticationcommons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

public class JwtUtils {
    public static String getSubIdFromAuthHeader(String authHeader){
        return GetDecodedPayload(authHeader).get("sub").asText();
    }

    public static String getPreferredUsernameFromAuthHeader(String authHeader){
        return GetDecodedPayload(authHeader).get("preferred_username").asText();
    }

    private static JsonNode GetDecodedPayload(String authHeader){
        String token = authHeader.replace("Bearer ", "");
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        try {
            return new ObjectMapper().readTree(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage());
        }
    }
}
