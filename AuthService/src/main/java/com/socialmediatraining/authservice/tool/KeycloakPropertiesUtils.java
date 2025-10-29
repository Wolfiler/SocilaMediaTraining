package com.socialmediatraining.authservice.tool;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakPropertiesUtils {

    @Value("${keycloak.auth-server-url}")
    @Getter
    public String authServerUrl;

    @Value("${keycloak.realm}")
    @Getter
    public String realm;

    @Value("${keycloak.resource}")
    @Getter
    public String clientId;

    @Value("${keycloak.credentials.secret}")
    @Getter
    public String clientSecret;

    @Value("${keycloak.auth.jwk-set-uri}")
    @Getter
    public String jwkSetUri;
}
