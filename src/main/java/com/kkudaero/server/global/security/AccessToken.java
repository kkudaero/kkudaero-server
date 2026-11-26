package com.kkudaero.server.global.security;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
