package com.kkudaero.server.domain.calculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

import com.kkudaero.server.domain.calculator.dto.DutyEstimateRequest;
import com.kkudaero.server.domain.calculator.dto.DutyEstimateResponse;
import com.kkudaero.server.domain.calculator.dto.ExchangeRateSource;
import com.kkudaero.server.domain.calculator.error.CalculatorErrorCode;
import com.kkudaero.server.global.error.BusinessException;
import com.kkudaero.server.infra.exchange.CurrencyCode;
import com.kkudaero.server.infra.exchange.ExchangeRate;
import com.kkudaero.server.infra.exchange.ExchangeRateClient;
import com.kkudaero.server.infra.exchange.ExchangeRateUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatorService {

    private static final int USD_SCALE = 2;
    private static final int KRW_SCALE = 0;
    private static final int PERCENT_SCALE = 2;
    private static final int PERCENT_SHIFT = 2;

    private final ExchangeRateClient exchangeRateClient;

    public DutyEstimateResponse calculateDutyEstimate(DutyEstimateRequest request) {
        AppliedRates rates = resolveRates(request);
        BigDecimal itemPriceJpy = request.itemPriceJpy().add(request.localShippingFeeJpy());
        BigDecimal itemPriceKrw = itemPriceJpy.multiply(rates.jpyToKrw());
        BigDecimal combinedPriceKrw = itemPriceJpy.add(request.sameDayOtherItemPriceJpy())
                .multiply(rates.jpyToKrw());
        BigDecimal limitKrw = DutyPolicy.LIST_CLEARANCE_LIMIT_USD.multiply(rates.usdToKrw());

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
                itemPriceKrw.divide(rates.usdToKrw(), USD_SCALE, RoundingMode.HALF_UP),
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
                rates.jpyToKrw(),
                rates.usdToKrw(),
                rates.source(),
                rates.appliedFrom(),
                rates.fetchedAt()
        );
    }

    private AppliedRates resolveRates(DutyEstimateRequest request) {
        if (request.hasExchangeRates()) {
            return new AppliedRates(
                    request.jpyToKrwRate(),
                    request.usdToKrwRate(),
                    ExchangeRateSource.SELLER_INPUT,
                    null,
                    null
            );
        }

        try {
            ExchangeRate jpy = exchangeRateClient.getImportRate(CurrencyCode.JPY);
            ExchangeRate usd = exchangeRateClient.getImportRate(CurrencyCode.USD);
            return new AppliedRates(
                    jpy.krwPerUnit(),
                    usd.krwPerUnit(),
                    ExchangeRateSource.CUSTOMS,
                    jpy.appliedFrom(),
                    jpy.fetchedAt()
            );
        } catch (ExchangeRateUnavailableException exception) {
            log.warn("과세환율을 쓸 수 없습니다: {}", exception.getMessage());
            throw new BusinessException(CalculatorErrorCode.EXCHANGE_RATE_UNAVAILABLE);
        }
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal percent, RoundingMode roundingMode) {
        return amount.multiply(percent).movePointLeft(PERCENT_SHIFT).setScale(KRW_SCALE, roundingMode);
    }

    private record AppliedRates(
            BigDecimal jpyToKrw,
            BigDecimal usdToKrw,
            ExchangeRateSource source,
            LocalDate appliedFrom,
            Instant fetchedAt
    ) {
    }
}
