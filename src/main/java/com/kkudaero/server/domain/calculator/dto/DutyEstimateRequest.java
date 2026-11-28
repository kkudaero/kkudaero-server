package com.kkudaero.server.domain.calculator.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record DutyEstimateRequest(
        @Schema(description = "일본 판매처에 낸 물품 금액(엔)", example = "18000")
        @NotNull @PositiveOrZero @Digits(integer = AMOUNT_INTEGER_DIGITS, fraction = 0)
        BigDecimal itemPriceJpy,

        @Schema(description = "일본 내 운임(엔). 물품가격에 포함됩니다.", example = "800")
        @NotNull @PositiveOrZero @Digits(integer = AMOUNT_INTEGER_DIGITS, fraction = 0)
        BigDecimal localShippingFeeJpy,

        @Schema(description = "같은 날 같은 수취인 이름으로 따로 구매한 다른 물품가격 합계(엔). 없으면 0", example = "0")
        @NotNull @PositiveOrZero @Digits(integer = AMOUNT_INTEGER_DIGITS, fraction = 0)
        BigDecimal sameDayOtherItemPriceJpy,

        @Schema(description = "한국까지의 국제 배송비(원)", example = "12000")
        @NotNull @PositiveOrZero @Digits(integer = AMOUNT_INTEGER_DIGITS, fraction = 0)
        BigDecimal internationalShippingFeeKrw,

        @Schema(description = "품목의 관세율(%). 셀러가 확인한 값을 넣습니다.", example = "8")
        @NotNull @PositiveOrZero @DecimalMax(PERCENT_MAX)
        @Digits(integer = PERCENT_INTEGER_DIGITS, fraction = PERCENT_FRACTION_DIGITS)
        BigDecimal dutyRatePercent,

        @Schema(description = "1엔당 원화 환율. 달러 환율과 함께 비우면 관세청 과세환율을 씁니다.", example = "9.2")
        @Positive @Digits(integer = RATE_INTEGER_DIGITS, fraction = RATE_FRACTION_DIGITS)
        BigDecimal jpyToKrwRate,

        @Schema(description = "1달러당 원화 환율. 엔 환율과 함께 비우면 관세청 과세환율을 씁니다.", example = "1380")
        @Positive @Digits(integer = RATE_INTEGER_DIGITS, fraction = RATE_FRACTION_DIGITS)
        BigDecimal usdToKrwRate,

        @Schema(description = "고객에게 받는 판매가(원)", example = "250000")
        @NotNull @Positive @Digits(integer = AMOUNT_INTEGER_DIGITS, fraction = 0)
        BigDecimal sellingPriceKrw,

        @Schema(description = "판매 채널 수수료율(%)", example = "5.5")
        @NotNull @PositiveOrZero @DecimalMax(PERCENT_MAX)
        @Digits(integer = PERCENT_INTEGER_DIGITS, fraction = PERCENT_FRACTION_DIGITS)
        BigDecimal salesFeeRatePercent,

        @Schema(description = "관부가세를 셀러가 부담하면 true. true면 마진에서 예상 관부가세를 뺍니다.", example = "false")
        @NotNull
        Boolean isTaxPaidBySeller
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "엔 환율과 달러 환율은 둘 다 넣거나 둘 다 비워야 합니다")
    public boolean isExchangeRatePairValid() {
        return (jpyToKrwRate == null) == (usdToKrwRate == null);
    }

    public boolean hasExchangeRates() {
        return jpyToKrwRate != null && usdToKrwRate != null;
    }

    private static final int AMOUNT_INTEGER_DIGITS = 10;
    private static final int RATE_INTEGER_DIGITS = 5;
    private static final int RATE_FRACTION_DIGITS = 4;
    private static final int PERCENT_INTEGER_DIGITS = 3;
    private static final int PERCENT_FRACTION_DIGITS = 2;
    private static final String PERCENT_MAX = "100";
}
