package com.kkudaero.server.domain.customs.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.customs.error.CustomsErrorCode;
import com.kkudaero.server.domain.order.error.OrderErrorCode;
import com.kkudaero.server.global.error.GlobalErrorCode;
import com.kkudaero.server.infra.unipass.CargoEvent;
import com.kkudaero.server.infra.unipass.CargoStatus;
import com.kkudaero.server.infra.unipass.UnipassClient;
import com.kkudaero.server.infra.unipass.UnipassUnavailableException;
import com.kkudaero.server.support.SellerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomsApiTest {

    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String CUSTOMS_STATUS_PATH = "/api/v1/orders/{orderId}/customs-status";
    private static final String TRACKING_NUMBER = "5507410484";
    private static final Instant PROCESSED_AT = Instant.parse("2026-11-28T01:10:09Z");
    private static final Instant FETCHED_AT = Instant.parse("2026-11-28T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnipassClient unipassClient;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "seller@example.com");
    }

    @Test
    void 주문의_운송장번호로_통관_진행을_조회한다() throws Exception {
        long orderId = createOrder(accessToken, TRACKING_NUMBER);
        when(unipassClient.getCargoStatus(TRACKING_NUMBER)).thenReturn(Optional.of(cargoStatus()));

        getCustomsStatus(accessToken, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.internationalTrackingNumber").value(TRACKING_NUMBER))
                .andExpect(jsonPath("$.data.cargoManagementNumber").value("26POZ06265I00110321"))
                .andExpect(jsonPath("$.data.clearanceStatus").value("통관목록심사완료"))
                .andExpect(jsonPath("$.data.progressStatus").value("반출완료"))
                .andExpect(jsonPath("$.data.processedAt").value("2026-11-28T01:10:09Z"))
                .andExpect(jsonPath("$.data.events.length()").value(1))
                .andExpect(jsonPath("$.data.events[0].type").value("반출신고"))
                .andExpect(jsonPath("$.data.fetchedAt").value("2026-11-28T03:00:00Z"));
    }

    @Test
    void 운송장번호가_없는_주문은_조회하지_않고_409를_받는다() throws Exception {
        long orderId = createOrder(accessToken, null);

        getCustomsStatus(accessToken, orderId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CustomsErrorCode.TRACKING_NUMBER_MISSING.name()));
        verifyNoInteractions(unipassClient);
    }

    @Test
    void 관세청에_정보가_없으면_404를_받는다() throws Exception {
        long orderId = createOrder(accessToken, TRACKING_NUMBER);
        when(unipassClient.getCargoStatus(TRACKING_NUMBER)).thenReturn(Optional.empty());

        getCustomsStatus(accessToken, orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CustomsErrorCode.CUSTOMS_STATUS_NOT_FOUND.name()));
    }

    @Test
    void 관세청_조회에_실패하면_503을_받는다() throws Exception {
        long orderId = createOrder(accessToken, TRACKING_NUMBER);
        when(unipassClient.getCargoStatus(TRACKING_NUMBER)).thenThrow(new UnipassUnavailableException("조회 실패"));

        getCustomsStatus(accessToken, orderId)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(CustomsErrorCode.CUSTOMS_STATUS_UNAVAILABLE.name()));
    }

    @Test
    void 다른_셀러의_주문은_조회하지_않고_404를_받는다() throws Exception {
        long orderId = createOrder(accessToken, TRACKING_NUMBER);
        String otherAccessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "other@example.com");

        getCustomsStatus(otherAccessToken, orderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(OrderErrorCode.ORDER_NOT_FOUND.name()));
        verifyNoInteractions(unipassClient);
    }

    @Test
    void 토큰_없이_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(CUSTOMS_STATUS_PATH, 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    private CargoStatus cargoStatus() {
        return new CargoStatus(
                "26POZ06265I00110321",
                "통관목록심사완료",
                "반출완료",
                PROCESSED_AT,
                List.of(new CargoEvent("반출신고", PROCESSED_AT, "특송물류센터", "목록통관특송물품 반출")),
                FETCHED_AT
        );
    }

    private ResultActions getCustomsStatus(String token, long orderId) throws Exception {
        return mockMvc.perform(get(CUSTOMS_STATUS_PATH, orderId)
                .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token));
    }

    private long createOrder(String token, String internationalTrackingNumber) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productName", "빈티지 카메라");
        body.put("purchaseSource", "메루카리");
        body.put("purchaseDate", "2026-11-20");
        body.put("itemPriceJpy", 21000);
        body.put("localShippingFeeJpy", 800);
        body.put("internationalTrackingNumber", internationalTrackingNumber);
        String response = mockMvc.perform(post(ORDERS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }
}
