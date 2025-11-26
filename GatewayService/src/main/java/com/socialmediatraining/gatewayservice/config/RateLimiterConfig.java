package com.socialmediatraining.gatewayservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;


@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(50, 200, 1);
    }

    @Bean
    @Qualifier("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 100, 1);
    }

    @Bean
    @Qualifier("userRateLimiter")
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(100, 500, 1);
    }

    @Bean
    @Qualifier("contentRateLimiter")
    public RedisRateLimiter contentRateLimiter() {
        return new RedisRateLimiter(100, 1000, 1);
    }

    @Bean
    @Qualifier("notificationRateLimiter")
    public RedisRateLimiter notificationRateLimiter() {
        return new RedisRateLimiter(50, 200, 1);
    }


    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                return Mono.just(apiKey);
            }
            return Mono.just(
                    exchange.getRequest()
                            .getRemoteAddress()
                            .getAddress()
                            .getHostAddress()
            );
        };
    }

}
