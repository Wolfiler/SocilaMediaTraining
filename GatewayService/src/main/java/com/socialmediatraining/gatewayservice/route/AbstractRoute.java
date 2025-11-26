package com.socialmediatraining.gatewayservice.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.function.Function;

@Slf4j
public abstract class AbstractRoute {

    protected RedisRateLimiter rateLimiters;

    protected KeyResolver userKeyResolver;

    @Autowired
    public AbstractRoute(RedisRateLimiter rateLimiters, KeyResolver userKeyResolver){
        this.rateLimiters = rateLimiters;
        this.userKeyResolver = userKeyResolver;
    }

    protected GatewayFilter applyRateLimit() {
        return new RequestRateLimiterGatewayFilterFactory(
                rateLimiters,
                userKeyResolver
        ).apply(c -> {
            c.setKeyResolver(userKeyResolver);
            c.setDenyEmptyKey(false);
            c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        });
    }

    protected GatewayFilter loggingFilter() {
        return (exchange, chain) -> {
            log.info("User Service - Request: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath());
            return chain.filter(exchange);
        };
    }
}
