package com.socialmediatraining.contentservice.config;

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
    public OpenAPI contentServiceOpenApiConfig() {
        return new OpenAPI().info(new Info()
                        .title("Content service API")
                        .description("Content service API, handling posts and comments creation, likes and user feed")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("lb://content-service")));
    }
}