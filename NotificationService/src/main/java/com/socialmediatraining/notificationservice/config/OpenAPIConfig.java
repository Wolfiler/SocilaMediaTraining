package com.socialmediatraining.notificationservice.config;

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
    public OpenAPI notificationServiceOpenApiConfig() {
        return new OpenAPI().info(new Info()
                        .title("Notification service API")
                        .description("Notification service API, handling notifications send to users inc ase of important events")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("lb://notification-service")));
    }
}
