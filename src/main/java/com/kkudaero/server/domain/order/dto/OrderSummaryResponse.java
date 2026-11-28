package com.kkudaero.server.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import com.kkudaero.server.global.util.MaskingUtil;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderSummaryResponse(
        @Schema(description = "주문 ID") Long id,
        @Schema(description = "상품명") String productName,
        @Schema(description = "구매처") String purchaseSource,
        @Schema(description = "구매일") LocalDate purchaseDate,
        @Schema(description = "물품 금액(엔)") BigDecimal itemPriceJpy,
        @Schema(description = "수취인 이름", nullable = true) String recipientName,
        @Schema(description = "가린 개인통관고유부호. 전체 값은 상세 조회에서 줍니다.", nullable = true)
        String maskedPersonalCustomsCode,
        @Schema(description = "국제 운송장번호", nullable = true) String internationalTrackingNumber,
        @Schema(description = "진행 상태") OrderStatus status,
        @Schema(description = "자동 조회로 마지막에 확인한 통관 상태. 관세청이 준 값 그대로입니다.", nullable = true)
        String customsStatus,
        @Schema(description = "마지막 수정 시각(UTC)") Instant updatedAt
) {

    public static OrderSummaryResponse from(Order order) {
        String personalCustomsCode = order.getPersonalCustomsCode();
        return new OrderSummaryResponse(
                order.getId(),
                order.getProductName(),
                order.getPurchaseSource(),
                order.getPurchaseDate(),
                order.getItemPriceJpy(),
                order.getRecipientName(),
                personalCustomsCode == null ? null : MaskingUtil.maskPersonalCustomsCode(personalCustomsCode),
                order.getInternationalTrackingNumber(),
                order.getStatus(),
                order.getCustomsStatus(),
                order.getUpdatedAt()
        );
    }
}
