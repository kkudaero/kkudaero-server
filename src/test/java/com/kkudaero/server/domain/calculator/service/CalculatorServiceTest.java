package com.kkudaero.server.domain.calculator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.junit.jupiter.api.Test;

class CalculatorServiceTest {

    private static final String JPY_TO_KRW_RATE = "10";
    private static final String USD_TO_KRW_RATE = "1000";
    private static final String NO_AMOUNT = "0";

    private static final LocalDate APPLIED_FROM = LocalDate.of(2026, 11, 22);
    private static final Instant FETCHED_AT = Instant.parse("2026-11-27T16:00:00Z");

    private final ExchangeRateClient exchangeRateClient = mock(ExchangeRateClient.class);
    private final CalculatorService calculatorService = new CalculatorService(exchangeRateClient);

    @Test
    void 물품가격이_149_99달러면_목록통관이고_세금이_없다() {
        DutyEstimateResponse response = estimate("14999", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.itemPriceUsd()).isEqualByComparingTo("149.99");
        assertThat(response.isListClearance()).isTrue();
        assertThat(response.estimatedTaxKrw()).isEqualByComparingTo(NO_AMOUNT);
    }

    @Test
    void 물품가격이_150달러면_목록통관이다() {
        DutyEstimateResponse response = estimate("15000", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.itemPriceUsd()).isEqualByComparingTo("150");
        assertThat(response.isListClearance()).isTrue();
        assertThat(response.taxableAmountKrw()).isEqualByComparingTo(NO_AMOUNT);
    }

    @Test
    void 물품가격이_150_01달러면_목록통관이_아니고_전체_금액에_과세한다() {
        DutyEstimateResponse response = estimate("15001", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.itemPriceUsd()).isEqualByComparingTo("150.01");
        assertThat(response.isListClearance()).isFalse();
        assertThat(response.taxableAmountKrw()).isEqualByComparingTo("160010");
        assertThat(response.estimatedDutyKrw()).isEqualByComparingTo("12800");
        assertThat(response.estimatedVatKrw()).isEqualByComparingTo("17281");
    }

    @Test
    void 일본_내_운임은_물품가격에_포함된다() {
        DutyEstimateResponse response = estimate("14500", "501", NO_AMOUNT, false);

        assertThat(response.itemPriceUsd()).isEqualByComparingTo("150.01");
        assertThat(response.isListClearance()).isFalse();
    }

    @Test
    void 같은_날_구매분과_합쳐_기준을_넘으면_합산과세_위험을_알린다() {
        DutyEstimateResponse response = estimate("10000", NO_AMOUNT, "5001", false);

        assertThat(response.isListClearance()).isTrue();
        assertThat(response.isCombinedTaxationRisk()).isTrue();
    }

    @Test
    void 같은_날_구매분과_합쳐도_기준_이하면_합산과세_위험이_없다() {
        DutyEstimateResponse response = estimate("10000", NO_AMOUNT, "5000", false);

        assertThat(response.isCombinedTaxationRisk()).isFalse();
    }

    @Test
    void 이미_과세_대상이면_합산과세_위험으로_표시하지_않는다() {
        DutyEstimateResponse response = estimate("21000", NO_AMOUNT, "5000", false);

        assertThat(response.isListClearance()).isFalse();
        assertThat(response.isCombinedTaxationRisk()).isFalse();
    }

    @Test
    void 마진은_판매가에서_매입_원가와_배송비와_수수료를_뺀다() {
        DutyEstimateResponse response = estimate("21000", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.purchaseCostKrw()).isEqualByComparingTo("210000");
        assertThat(response.salesFeeKrw()).isEqualByComparingTo("16500");
        assertThat(response.estimatedMarginKrw()).isEqualByComparingTo("63500");
        assertThat(response.estimatedMarginRatePercent()).isEqualByComparingTo("21.17");
    }

    @Test
    void 셀러가_관부가세를_부담하면_마진에서_예상_세액을_뺀다() {
        DutyEstimateResponse response = estimate("21000", NO_AMOUNT, NO_AMOUNT, true);

        assertThat(response.estimatedTaxKrw()).isEqualByComparingTo("41360");
        assertThat(response.estimatedMarginKrw()).isEqualByComparingTo("22140");
        assertThat(response.estimatedMarginRatePercent()).isEqualByComparingTo("7.38");
    }

    @Test
    void 계산에_쓴_환율과_기준_금액을_함께_돌려준다() {
        DutyEstimateResponse response = estimate("21000", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.jpyToKrwRate()).isEqualByComparingTo(JPY_TO_KRW_RATE);
        assertThat(response.usdToKrwRate()).isEqualByComparingTo(USD_TO_KRW_RATE);
        assertThat(response.listClearanceLimitUsd()).isEqualByComparingTo(DutyPolicy.LIST_CLEARANCE_LIMIT_USD);
    }

    @Test
    void 환율을_직접_넣으면_과세환율을_조회하지_않는다() {
        DutyEstimateResponse response = estimate("21000", NO_AMOUNT, NO_AMOUNT, false);

        assertThat(response.exchangeRateSource()).isEqualTo(ExchangeRateSource.SELLER_INPUT);
        assertThat(response.exchangeRateAppliedFrom()).isNull();
        assertThat(response.exchangeRateFetchedAt()).isNull();
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void 환율을_비우면_관세청_과세환율로_계산하고_기준_시각을_돌려준다() {
        when(exchangeRateClient.getImportRate(CurrencyCode.JPY)).thenReturn(rate(CurrencyCode.JPY, JPY_TO_KRW_RATE));
        when(exchangeRateClient.getImportRate(CurrencyCode.USD)).thenReturn(rate(CurrencyCode.USD, USD_TO_KRW_RATE));

        DutyEstimateResponse response = calculatorService.calculateDutyEstimate(requestWithoutRates());

        assertThat(response.itemPriceUsd()).isEqualByComparingTo("150.01");
        assertThat(response.isListClearance()).isFalse();
        assertThat(response.jpyToKrwRate()).isEqualByComparingTo(JPY_TO_KRW_RATE);
        assertThat(response.usdToKrwRate()).isEqualByComparingTo(USD_TO_KRW_RATE);
        assertThat(response.exchangeRateSource()).isEqualTo(ExchangeRateSource.CUSTOMS);
        assertThat(response.exchangeRateAppliedFrom()).isEqualTo(APPLIED_FROM);
        assertThat(response.exchangeRateFetchedAt()).isEqualTo(FETCHED_AT);
    }

    @Test
    void 과세환율을_조회하지_못하면_환율_조회_실패_오류를_던진다() {
        when(exchangeRateClient.getImportRate(CurrencyCode.JPY))
                .thenThrow(new ExchangeRateUnavailableException("조회 실패"));

        assertThatThrownBy(() -> calculatorService.calculateDutyEstimate(requestWithoutRates()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CalculatorErrorCode.EXCHANGE_RATE_UNAVAILABLE);
    }

    private ExchangeRate rate(CurrencyCode currency, String krwPerUnit) {
        return new ExchangeRate(currency, new BigDecimal(krwPerUnit), APPLIED_FROM, FETCHED_AT);
    }

    private DutyEstimateRequest requestWithoutRates() {
        return new DutyEstimateRequest(
                new BigDecimal("15001"),
                new BigDecimal(NO_AMOUNT),
                new BigDecimal(NO_AMOUNT),
                new BigDecimal("10000"),
                new BigDecimal("8"),
                null,
                null,
                new BigDecimal("300000"),
                new BigDecimal("5.5"),
                false
        );
    }

    private DutyEstimateResponse estimate(
            String itemPriceJpy,
            String localShippingFeeJpy,
            String sameDayOtherItemPriceJpy,
            boolean isTaxPaidBySeller
    ) {
        return calculatorService.calculateDutyEstimate(new DutyEstimateRequest(
                new BigDecimal(itemPriceJpy),
                new BigDecimal(localShippingFeeJpy),
                new BigDecimal(sameDayOtherItemPriceJpy),
                new BigDecimal("10000"),
                new BigDecimal("8"),
                new BigDecimal(JPY_TO_KRW_RATE),
                new BigDecimal(USD_TO_KRW_RATE),
                new BigDecimal("300000"),
                new BigDecimal("5.5"),
                isTaxPaidBySeller
        ));
    }
}
