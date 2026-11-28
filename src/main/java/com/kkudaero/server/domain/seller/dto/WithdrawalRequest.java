package com.kkudaero.server.domain.seller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record WithdrawalRequest(
        @Schema(description = "본인 확인용 현재 비밀번호")
        @NotBlank String password
) {
}
