package com.kkudaero.server.infra.unipass;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kkudaero.unipass")
public record UnipassProperties(
        String cargoProgressUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl,
        int cacheMaxSize
) {

    public UnipassProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
    }
}
