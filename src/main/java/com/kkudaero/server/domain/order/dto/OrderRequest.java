package com.kkudaero.server.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record OrderRequest(
        @Schema(description = "상품명")
        @NotBlank @Size(max = Order.PRODUCT_NAME_MAX_LENGTH) String productName,

        @Schema(description = "구매처", example = "메루카리")
        @NotBlank @Size(max = Order.PURCHASE_SOURCE_MAX_LENGTH) String purchaseSource,

        @Schema(description = "구매일", example = "2026-11-28")
        @NotNull LocalDate purchaseDate,

        @Schema(description = "물품 금액(엔)")
        @NotNull @PositiveOrZero @Digits(integer = Order.PRICE_INTEGER_DIGITS, fraction = 0)
        BigDecimal itemPriceJpy,

        @Schema(description = "일본 내 운임(엔)")
        @NotNull @PositiveOrZero @Digits(integer = Order.PRICE_INTEGER_DIGITS, fraction = 0)
        BigDecimal localShippingFeeJpy,

        @Schema(description = "수취인 이름. 암호화해서 저장합니다.", nullable = true)
        @Size(max = Order.RECIPIENT_NAME_MAX_LENGTH) String recipientName,

        @Schema(description = "개인통관고유부호. P와 숫자 12자리. 암호화해서 저장합니다.", nullable = true)
        @Pattern(regexp = PERSONAL_CUSTOMS_CODE_PATTERN, message = "P와 숫자 12자리여야 합니다")
        String personalCustomsCode,

        @Schema(description = "일본 내 송장번호", nullable = true)
        @Size(max = Order.TRACKING_NUMBER_MAX_LENGTH) String localTrackingNumber,

        @Schema(description = "국제 운송장번호", nullable = true)
        @Size(max = Order.TRACKING_NUMBER_MAX_LENGTH) String internationalTrackingNumber,

        @Schema(description = "메모", nullable = true)
        @Size(max = Order.MEMO_MAX_LENGTH) String memo
) {

    private static final String PERSONAL_CUSTOMS_CODE_PATTERN = "^P\\d{12}$";

    public OrderRequest {
        productName = normalize(productName);
        purchaseSource = normalize(purchaseSource);
        recipientName = normalize(recipientName);
        personalCustomsCode = normalize(personalCustomsCode);
        localTrackingNumber = normalize(localTrackingNumber);
        internationalTrackingNumber = normalize(internationalTrackingNumber);
        memo = normalize(memo);
    }

    public OrderDetails toDetails() {
        return new OrderDetails(
                productName,
                purchaseSource,
                purchaseDate,
                itemPriceJpy,
                localShippingFeeJpy,
                recipientName,
                personalCustomsCode,
                localTrackingNumber,
                internationalTrackingNumber,
                memo
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
