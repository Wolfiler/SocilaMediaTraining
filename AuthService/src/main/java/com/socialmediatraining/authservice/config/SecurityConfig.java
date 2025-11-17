package com.socialmediatraining.authservice.config;

import com.socialmediatraining.authenticationcommons.RoleUtils;
import com.socialmediatraining.authservice.handler.CustomLogoutHandler;
import com.socialmediatraining.authservice.tool.JwtAuthConverter;
import com.socialmediatraining.authservice.tool.KeycloakPropertiesUtils;
import lombok.Getter;
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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Getter
    private final static String[] authorizedPaths = {"/api/v1/auth/signup","/api/v1/auth/signin"};
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthConverter());

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequest -> {
                    authorizeRequest.requestMatchers(authorizedPaths).permitAll();
                    authorizeRequest.requestMatchers(adminPaths).hasAnyRole(RoleUtils.GetAdminRoleNamesAsList().toArray(new String[0]));
                    authorizeRequest.requestMatchers(userPaths).hasAnyRole(RoleUtils.GetUserRoleNamesAsList().toArray(new String[0]));
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
        return NimbusJwtDecoder.withJwkSetUri(keycloakProperties.getJwkSetUri()).build();
    }
}
