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
public class ContentServiceRoute extends AbstractRoute {

    @Autowired
    public ContentServiceRoute(@Qualifier("contentRateLimiter") RedisRateLimiter rateLimiters, KeyResolver userKeyResolver) {
        super(rateLimiters, userKeyResolver);
    }

    @Bean
    public RouteLocator contentRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("content-service-profile-posts", route -> route
                        .path("/api/v1/profile/*/posts/**")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .filter(applyRateLimit())
                        )
                        .uri("lb://content-service")
                )
                .route("content-service-posts", route -> route
                        .path("/api/v1/posts/**")
                        .and().method(GET, POST, PUT, DELETE)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .filter(applyRateLimit())
                        )
                        .uri("lb://content-service")
                )
                .route("content-service-like", route -> route
                        .path("/api/v1/like/**")
                        .and().method(POST, DELETE)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiters)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                )
                        )
                        .uri("lb://content-service")
                )
                .route("content-service-feed", route -> route
                        .path("/api/v1/feed/**")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiters)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                )
                        )
                        .uri("lb://content-service")
                )
                .route("content-service", route -> route
                        .path("/content-service/v3/api-docs")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                        )
                        .uri("lb://content-service")
                )
                .build();
    }
}
