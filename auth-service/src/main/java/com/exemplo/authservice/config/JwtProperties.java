package com.exemplo.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration accessExpiration, Duration refreshExpiration) {
}
