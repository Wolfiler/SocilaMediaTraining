package com.socialmediatraining.gatewayservice.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import static jakarta.ws.rs.HttpMethod.*;

@Configuration
@Slf4j
public class NotificationRoute extends AbstractRoute {
    @Autowired
    public NotificationRoute(@Qualifier("notificationRateLimiter") RedisRateLimiter rateLimiters, KeyResolver userKeyResolver) {
        super(rateLimiters, userKeyResolver);
    }

    @Bean
    public RouteLocator notificationRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("notification-service", route -> route
                        .path("/api/v1/notifications/**")
                        .and().method(GET, POST, PUT, DELETE)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiters)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                )
                        )
                        .uri("lb://notification-service")
                )
                .route("notification-service", route -> route
                        .path("/notification-service/v3/api-docs")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                        )
                        .uri("lb://notification-service")
                )
                .build();
    }
}
