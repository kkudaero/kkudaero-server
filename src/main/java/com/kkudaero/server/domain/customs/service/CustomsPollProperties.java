package com.kkudaero.server.domain.customs.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kkudaero.customs")
public record CustomsPollProperties(Duration pollMaxOrderAge) {
}
