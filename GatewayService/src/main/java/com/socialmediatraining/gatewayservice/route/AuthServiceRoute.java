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
public class AuthServiceRoute extends AbstractRoute {

    @Autowired
    public AuthServiceRoute(@Qualifier("authRateLimiter") RedisRateLimiter  authRateLimiter, KeyResolver userKeyResolver) {
        super(authRateLimiter, userKeyResolver);
    }

    @Bean
    public RouteLocator authRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authentication-service", route -> route
                        .path("/api/v1/auth/**")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                                .filter(applyRateLimit())
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(rateLimiters)
                                        .setKeyResolver(userKeyResolver)
                                        .setDenyEmptyKey(false)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                )
                        )
                        .uri("lb://authentication-service")
                )
                .route("authentication-service", route -> route
                        .path("/api/v1/user/**")
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
                        .uri("lb://authentication-service")
                )
                .route("authentication-service", route -> route
                        .path("/authentication-service/v3/api-docs")
                        .and().method(GET)
                        .filters(filter -> filter
                                .filter(loggingFilter())
                        )
                        .uri("lb://authentication-service")
                )
                .build();
    }
}
