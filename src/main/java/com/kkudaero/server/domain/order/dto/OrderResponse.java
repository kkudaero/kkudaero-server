package com.kkudaero.server.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderResponse(
        @Schema(description = "주문 ID") Long id,
        @Schema(description = "상품명") String productName,
        @Schema(description = "구매처") String purchaseSource,
        @Schema(description = "구매일") LocalDate purchaseDate,
        @Schema(description = "물품 금액(엔)") BigDecimal itemPriceJpy,
        @Schema(description = "일본 내 운임(엔)") BigDecimal localShippingFeeJpy,
        @Schema(description = "수취인 이름", nullable = true) String recipientName,
        @Schema(description = "개인통관고유부호", nullable = true) String personalCustomsCode,
        @Schema(description = "일본 내 송장번호", nullable = true) String localTrackingNumber,
        @Schema(description = "국제 운송장번호", nullable = true) String internationalTrackingNumber,
        @Schema(description = "진행 상태") OrderStatus status,
        @Schema(description = "메모", nullable = true) String memo,
        @Schema(description = "등록 시각(UTC)") Instant createdAt,
        @Schema(description = "마지막 수정 시각(UTC)") Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getPurchaseSource(),
                order.getPurchaseDate(),
                order.getItemPriceJpy(),
                order.getLocalShippingFeeJpy(),
                order.getRecipientName(),
                order.getPersonalCustomsCode(),
                order.getLocalTrackingNumber(),
                order.getInternationalTrackingNumber(),
                order.getStatus(),
                order.getMemo(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
