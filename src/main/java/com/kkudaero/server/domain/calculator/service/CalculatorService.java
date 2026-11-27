package com.kkudaero.server.domain.calculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.kkudaero.server.domain.calculator.dto.DutyEstimateRequest;
import com.kkudaero.server.domain.calculator.dto.DutyEstimateResponse;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    private static final int USD_SCALE = 2;
    private static final int KRW_SCALE = 0;
    private static final int PERCENT_SCALE = 2;
    private static final int PERCENT_SHIFT = 2;

    public DutyEstimateResponse calculateDutyEstimate(DutyEstimateRequest request) {
        BigDecimal itemPriceJpy = request.itemPriceJpy().add(request.localShippingFeeJpy());
        BigDecimal itemPriceKrw = itemPriceJpy.multiply(request.jpyToKrwRate());
        BigDecimal combinedPriceKrw = itemPriceJpy.add(request.sameDayOtherItemPriceJpy())
                .multiply(request.jpyToKrwRate());
        BigDecimal limitKrw = DutyPolicy.LIST_CLEARANCE_LIMIT_USD.multiply(request.usdToKrwRate());

        boolean isListClearance = itemPriceKrw.compareTo(limitKrw) <= 0;
        boolean isCombinedTaxationRisk = isListClearance && combinedPriceKrw.compareTo(limitKrw) > 0;

        BigDecimal taxableAmountKrw = isListClearance
                ? BigDecimal.ZERO
                : itemPriceKrw.add(request.internationalShippingFeeKrw()).setScale(KRW_SCALE, RoundingMode.FLOOR);
        BigDecimal dutyKrw = percentOf(taxableAmountKrw, request.dutyRatePercent(), RoundingMode.FLOOR);
        BigDecimal vatKrw = percentOf(taxableAmountKrw.add(dutyKrw), DutyPolicy.VAT_RATE_PERCENT, RoundingMode.FLOOR);
        BigDecimal taxKrw = dutyKrw.add(vatKrw);

        BigDecimal purchaseCostKrw = itemPriceKrw.setScale(KRW_SCALE, RoundingMode.HALF_UP);
        BigDecimal salesFeeKrw = percentOf(
                request.sellingPriceKrw(),
                request.salesFeeRatePercent(),
                RoundingMode.HALF_UP
        );
        BigDecimal marginKrw = request.sellingPriceKrw()
                .subtract(purchaseCostKrw)
                .subtract(request.internationalShippingFeeKrw())
                .subtract(salesFeeKrw)
                .subtract(request.isTaxPaidBySeller() ? taxKrw : BigDecimal.ZERO);

        return new DutyEstimateResponse(
                itemPriceKrw.divide(request.usdToKrwRate(), USD_SCALE, RoundingMode.HALF_UP),
                DutyPolicy.LIST_CLEARANCE_LIMIT_USD,
                isListClearance,
                isCombinedTaxationRisk,
                taxableAmountKrw,
                dutyKrw,
                vatKrw,
                taxKrw,
                purchaseCostKrw,
                salesFeeKrw,
                marginKrw,
                marginKrw.scaleByPowerOfTen(PERCENT_SHIFT)
                        .divide(request.sellingPriceKrw(), PERCENT_SCALE, RoundingMode.HALF_UP),
                request.jpyToKrwRate(),
                request.usdToKrwRate()
        );
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal percent, RoundingMode roundingMode) {
        return amount.multiply(percent).movePointLeft(PERCENT_SHIFT).setScale(KRW_SCALE, roundingMode);
    }
}
