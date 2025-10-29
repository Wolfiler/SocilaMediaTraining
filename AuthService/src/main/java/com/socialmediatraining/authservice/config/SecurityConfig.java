package com.socialmediatraining.authservice.config;

import com.socialmediatraining.authservice.handler.CustomLogoutHandler;
import com.socialmediatraining.authservice.tool.JwtAuthConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final static String[] authorizedPaths = {"/api/v1/auth/login","/api/v1/auth/signup","/api/v1/auth/logout","/api/v1/auth/welcome-new-user"};
    private final static String[] adminPaths = {"/api/v1/auth/admin/**"};
    private final static String[] defaultApiPaths = {"/api/v1/auth/**"};
    private final static String[] userPaths = {"/api/v1/auth/user/**"};
    private final CustomLogoutHandler customLogoutHandler;
    private final KeycloakPropertiesUtils keycloakProperties;

    @Autowired
    public SecurityConfig(CustomLogoutHandler customLogoutHandler, KeycloakPropertiesUtils keycloakProperties) {
        this.customLogoutHandler = customLogoutHandler;
        this.keycloakProperties = keycloakProperties;
    }

    private enum role {
        USER,ADMIN
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthConverter());

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequest -> {
                    authorizeRequest.requestMatchers(authorizedPaths).permitAll();
                    authorizeRequest.requestMatchers(adminPaths).hasRole(role.ADMIN.name());
                    authorizeRequest.requestMatchers(userPaths).hasAnyRole(role.ADMIN.name(),role.USER.name());
                    authorizeRequest.requestMatchers(defaultApiPaths).authenticated();
                    authorizeRequest.anyRequest().authenticated();
                })
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtConverter)
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessUrl("/api/v1/auth/signin")
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .deleteCookies("AUTH_SESSION_ID")
                )
        ;
        return httpSecurity.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(keycloakProperties.jwkSetUri).build();
    }
}
