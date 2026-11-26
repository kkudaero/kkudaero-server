package com.kkudaero.server.domain.seller.dto;

import java.time.Instant;

import com.kkudaero.server.global.security.AccessToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Authorization 헤더에 Bearer로 넣을 토큰") String accessToken,
        @Schema(description = "토큰 만료 시각(UTC)") Instant expiresAt
) {

    public static LoginResponse from(AccessToken accessToken) {
        return new LoginResponse(accessToken.value(), accessToken.expiresAt());
    }
}
