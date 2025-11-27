package com.socialmediatraining.authservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@OpenAPIDefinition
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI authServiceOpenApiConfig() {
        return new OpenAPI().info(new Info()
                .title("Authentication service API")
                .description("Authentication service API, handling authentication of users on the Social media platform")
                .version("1.0.0"))
                .servers(List.of(new Server().url("lb://authentication-service")));
    }
}
