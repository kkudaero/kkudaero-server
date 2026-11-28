package com.kkudaero.server.global.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kkudaero.encryption")
public record EncryptionProperties(String key) {
}
