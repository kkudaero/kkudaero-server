package com.kkudaero.server.domain.calculator.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.calculator.error.CalculatorErrorCode;
import com.kkudaero.server.global.error.GlobalErrorCode;
import com.kkudaero.server.infra.exchange.CurrencyCode;
import com.kkudaero.server.infra.exchange.ExchangeRate;
import com.kkudaero.server.infra.exchange.ExchangeRateClient;
import com.kkudaero.server.infra.exchange.ExchangeRateUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalculatorApiTest {

    private static final String DUTY_ESTIMATES_PATH = "/api/v1/duty-estimates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeRateClient exchangeRateClient;

    @Test
    void 로그인한_셀러는_예상_관부가세와_마진을_계산한다() throws Exception {
        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isListClearance").value(false))
                .andExpect(jsonPath("$.data.isCombinedTaxationRisk").value(false))
                .andExpect(jsonPath("$.data.estimatedTaxKrw").value(41360))
                .andExpect(jsonPath("$.data.estimatedMarginKrw").value(63500))
                .andExpect(jsonPath("$.data.usdToKrwRate").value(1000))
                .andExpect(jsonPath("$.data.exchangeRateSource").value("SELLER_INPUT"));
    }

    @Test
    void 환율을_비우면_관세청_과세환율로_계산한다() throws Exception {
        Instant fetchedAt = Instant.parse("2026-11-27T16:00:00Z");
        LocalDate appliedFrom = LocalDate.of(2026, 11, 22);
        when(exchangeRateClient.getImportRate(CurrencyCode.JPY))
                .thenReturn(new ExchangeRate(CurrencyCode.JPY, BigDecimal.TEN, appliedFrom, fetchedAt));
        when(exchangeRateClient.getImportRate(CurrencyCode.USD))
                .thenReturn(new ExchangeRate(CurrencyCode.USD, new BigDecimal("1000"), appliedFrom, fetchedAt));
        Map<String, Object> body = validBody();
        body.remove("jpyToKrwRate");
        body.remove("usdToKrwRate");

        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedTaxKrw").value(41360))
                .andExpect(jsonPath("$.data.exchangeRateSource").value("CUSTOMS"))
                .andExpect(jsonPath("$.data.exchangeRateAppliedFrom").value("2026-11-22"))
                .andExpect(jsonPath("$.data.exchangeRateFetchedAt").value("2026-11-27T16:00:00Z"));
    }

    @Test
    void 과세환율을_조회하지_못하면_503을_받는다() throws Exception {
        when(exchangeRateClient.getImportRate(CurrencyCode.JPY))
                .thenThrow(new ExchangeRateUnavailableException("조회 실패"));
        Map<String, Object> body = validBody();
        body.remove("jpyToKrwRate");
        body.remove("usdToKrwRate");

        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(CalculatorErrorCode.EXCHANGE_RATE_UNAVAILABLE.name()));
    }

    @Test
    void 환율을_하나만_넣으면_400을_받는다() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("usdToKrwRate");

        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 토큰_없이_계산하면_401을_받는다() throws Exception {
        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    @Test
    void 환율이_0이면_400을_받는다() throws Exception {
        Map<String, Object> body = validBody();
        body.put("usdToKrwRate", 0);

        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 필수_값이_빠지면_400을_받는다() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("isTaxPaidBySeller");

        mockMvc.perform(post(DUTY_ESTIMATES_PATH)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("itemPriceJpy", 21000);
        body.put("localShippingFeeJpy", 0);
        body.put("sameDayOtherItemPriceJpy", 0);
        body.put("internationalShippingFeeKrw", 10000);
        body.put("dutyRatePercent", 8);
        body.put("jpyToKrwRate", 10);
        body.put("usdToKrwRate", 1000);
        body.put("sellingPriceKrw", 300000);
        body.put("salesFeeRatePercent", 5.5);
        body.put("isTaxPaidBySeller", false);
        return body;
    }
}
