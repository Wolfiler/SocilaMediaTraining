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
public class UserServiceRoute {

    @Bean
    public RouterFunction<ServerResponse> userRoutes() {
        return GatewayRouterFunctions.route("user-service")
                .GET("/api/v1/follow/**", http())
                .POST("/api/v1/follow/**", http())
                .DELETE("/api/v1/follow/**", http())
                .filter(lb("user-service"))
                .build().filter((request, next) -> {
                    log.info("Request: {}", request.uri());
                    return next.handle(request);
                });
    }
}
