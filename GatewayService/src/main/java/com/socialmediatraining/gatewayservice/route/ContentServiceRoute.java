package com.socialmediatraining.gatewayservice.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
@Slf4j
public class ContentServiceRoute {

    @Bean
    public RouterFunction<ServerResponse> contentServiceRoutes() {
        return GatewayRouterFunctions.route("content-service")
                .GET("/api/v1/profile/*/posts/**", http())

                .GET("/api/v1/posts/**", http())
                .POST("/api/v1/posts/**", http())
                .DELETE("/api/v1/posts/**", http())
                .PUT("/api/v1/posts/**", http())

                .GET("/api/v1/like/**", http())
                .POST("/api/v1/like/**", http())
                .DELETE("/api/v1/like/**", http())

                .filter(lb("content-service"))
                .build().filter((request, next) -> {
                    log.info("Request: {}", request.uri());
                    return next.handle(request);
                });
    }
}
