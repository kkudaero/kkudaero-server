package com.kkudaero.server.domain.order.dto;

import com.kkudaero.server.domain.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @Schema(description = "바꿀 상태") @NotNull OrderStatus status
) {
}
