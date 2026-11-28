package com.kkudaero.server.domain.calculator.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.global.error.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
                .andExpect(jsonPath("$.data.usdToKrwRate").value(1000));
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
