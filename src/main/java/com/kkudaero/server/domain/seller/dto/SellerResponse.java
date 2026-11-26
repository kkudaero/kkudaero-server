package com.kkudaero.server.domain.seller.dto;

import java.time.Instant;

import com.kkudaero.server.domain.seller.entity.Seller;
import io.swagger.v3.oas.annotations.media.Schema;

public record SellerResponse(
        @Schema(description = "셀러 ID") Long id,
        @Schema(description = "이메일") String email,
        @Schema(description = "가입 시각(UTC)") Instant createdAt
) {

    public static SellerResponse from(Seller seller) {
        return new SellerResponse(seller.getId(), seller.getEmail(), seller.getCreatedAt());
    }
}
