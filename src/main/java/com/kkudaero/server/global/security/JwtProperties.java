package com.kkudaero.server.global.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kkudaero.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {
}
