package com.kkudaero.server.domain.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderDetails(
        String productName,
        String purchaseSource,
        LocalDate purchaseDate,
        BigDecimal itemPriceJpy,
        BigDecimal localShippingFeeJpy,
        String recipientName,
        String personalCustomsCode,
        String localTrackingNumber,
        String internationalTrackingNumber,
        String memo
) {
}
