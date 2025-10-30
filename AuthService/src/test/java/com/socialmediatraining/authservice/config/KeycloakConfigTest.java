package com.socialmediatraining.authservice.config;

import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KeycloakConfigTest {
    @Mock
    private KeycloakPropertiesUtils keycloakProperties;
    private KeycloakConfig keycloakConfig;

    @BeforeEach
    void setUp() {
        keycloakConfig = new KeycloakConfig(keycloakProperties);
    }

    @Test
    void keycloak_should_be_correctly_instantiated() {
        String serverUrl = "http://localhost:8100";
        String realm = "test-realm";
        String clientId = "test-client";
        String clientSecret = "test-secret";

        given(keycloakProperties.getAuthServerUrl()).willReturn(serverUrl);
        given(keycloakProperties.getRealm()).willReturn(realm);
        given(keycloakProperties.getClientId()).willReturn(clientId);
        given(keycloakProperties.getClientSecret()).willReturn(clientSecret);

        Keycloak keycloak = keycloakConfig.keycloak();

        assertThat(keycloak).isNotNull();
    }

    @Test
    void keycloakConfigResolver_should_return_KeycloakSpringBootConfigResolver() {
        var resolver = keycloakConfig.keycloakConfigResolver();

        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(KeycloakSpringBootConfigResolver.class);
    }

}