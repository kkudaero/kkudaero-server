package com.kkudaero.server.infra.exchange;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kkudaero.exchange")
public record ExchangeProperties(String url, String serviceKey, Duration connectTimeout, Duration readTimeout) {

    public ExchangeProperties {
        serviceKey = serviceKey == null ? "" : serviceKey.strip();
    }
}
