package com.kkudaero.server.domain.calculator.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모든 금액은 응답에 담긴 환율로 계산한 예상치입니다.")
public record DutyEstimateResponse(
        @Schema(description = "물품가격(물품 금액 + 일본 내 운임)의 달러 환산값") BigDecimal itemPriceUsd,
        @Schema(description = "목록통관 기준 금액(달러)") BigDecimal listClearanceLimitUsd,
        @Schema(description = "물품가격이 기준 금액 이하라 관부가세가 면제되는지") boolean isListClearance,
        @Schema(description = "이 건은 기준 이하지만 같은 날 구매분과 합치면 기준을 넘는지") boolean isCombinedTaxationRisk,
        @Schema(description = "예상 과세가격(원). 면세면 0") BigDecimal taxableAmountKrw,
        @Schema(description = "예상 관세(원)") BigDecimal estimatedDutyKrw,
        @Schema(description = "예상 부가세(원)") BigDecimal estimatedVatKrw,
        @Schema(description = "예상 관세와 부가세의 합(원)") BigDecimal estimatedTaxKrw,
        @Schema(description = "매입 원가(원). 물품가격을 원화로 바꾼 값") BigDecimal purchaseCostKrw,
        @Schema(description = "판매 채널 수수료(원)") BigDecimal salesFeeKrw,
        @Schema(description = "예상 마진(원)") BigDecimal estimatedMarginKrw,
        @Schema(description = "판매가 대비 예상 마진율(%)") BigDecimal estimatedMarginRatePercent,
        @Schema(description = "계산에 쓴 1엔당 원화 환율") BigDecimal jpyToKrwRate,
        @Schema(description = "계산에 쓴 1달러당 원화 환율") BigDecimal usdToKrwRate,
        @Schema(description = "환율 출처. CUSTOMS는 관세청 과세환율, SELLER_INPUT은 요청에 담긴 값")
        ExchangeRateSource exchangeRateSource,
        @Schema(description = "과세환율 적용 개시일(한국 날짜). 셀러가 넣은 환율이면 null", nullable = true)
        LocalDate exchangeRateAppliedFrom,
        @Schema(description = "과세환율을 조회한 시각(UTC). 셀러가 넣은 환율이면 null", nullable = true)
        Instant exchangeRateFetchedAt
) {
}
