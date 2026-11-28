package com.kkudaero.server.domain.customs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.infra.unipass.CargoStatus;
import com.kkudaero.server.infra.unipass.UnipassClient;
import com.kkudaero.server.infra.unipass.UnipassUnavailableException;
import com.kkudaero.server.support.SellerTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomsStatusSchedulerTest {

    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String ORDER_PATH = "/api/v1/orders/{orderId}";
    private static final String ORDER_STATUS_PATH = "/api/v1/orders/{orderId}/status";
    private static final String NOTIFICATIONS_PATH = "/api/v1/notifications";
    private static final String TRACKING_NUMBER = "5507410484";
    private static final String OTHER_TRACKING_NUMBER = "5507410485";
    private static final String ARRIVED = "입항적하목록 심사완료";
    private static final String RELEASED = "반출완료";
    private static final Instant FETCHED_AT = Instant.parse("2026-11-28T03:00:00Z");
    private static final Duration OLDER_THAN_MAX_ORDER_AGE = Duration.ofDays(91);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomsStatusScheduler customsStatusScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UnipassClient unipassClient;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "seller@example.com");
    }

    @Test
    void 통관_상태를_처음_확인하면_주문에_저장하고_알림을_만든다() throws Exception {
        long orderId = createOrder(TRACKING_NUMBER);
        givenCargoStatus(TRACKING_NUMBER, ARRIVED);

        customsStatusScheduler.pollCustomsStatuses();

        perform(get(ORDER_PATH, orderId))
                .andExpect(jsonPath("$.data.customsStatus").value(ARRIVED))
                .andExpect(jsonPath("$.data.customsCheckedAt").value("2026-11-28T03:00:00Z"));
        perform(get(NOTIFICATIONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].type").value("CUSTOMS_STATUS_CHANGED"))
                .andExpect(jsonPath("$.data.notifications[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data.notifications[0].message")
                        .value("'빈티지 카메라' 주문의 통관 상태가 '입항적하목록 심사완료'(으)로 바뀌었습니다."))
                .andExpect(jsonPath("$.data.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void 상태가_그대로면_알림을_다시_만들지_않고_바뀌면_하나_더_만든다() throws Exception {
        createOrder(TRACKING_NUMBER);
        givenCargoStatus(TRACKING_NUMBER, ARRIVED);

        customsStatusScheduler.pollCustomsStatuses();
        customsStatusScheduler.pollCustomsStatuses();
        perform(get(NOTIFICATIONS_PATH)).andExpect(jsonPath("$.data.notifications.length()").value(1));

        givenCargoStatus(TRACKING_NUMBER, RELEASED);
        customsStatusScheduler.pollCustomsStatuses();

        perform(get(NOTIFICATIONS_PATH))
                .andExpect(jsonPath("$.data.notifications.length()").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    void 한_주문의_조회가_실패해도_다른_주문은_계속_조회한다() throws Exception {
        createOrder(TRACKING_NUMBER);
        long secondOrderId = createOrder(OTHER_TRACKING_NUMBER);
        when(unipassClient.getCargoStatus(TRACKING_NUMBER)).thenThrow(new UnipassUnavailableException("조회 실패"));
        givenCargoStatus(OTHER_TRACKING_NUMBER, ARRIVED);

        customsStatusScheduler.pollCustomsStatuses();

        perform(get(NOTIFICATIONS_PATH))
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].orderId").value(secondOrderId));
    }

    @Test
    void 관세청에_정보가_없으면_알림을_만들지_않는다() throws Exception {
        createOrder(TRACKING_NUMBER);
        when(unipassClient.getCargoStatus(TRACKING_NUMBER)).thenReturn(Optional.empty());

        customsStatusScheduler.pollCustomsStatuses();

        perform(get(NOTIFICATIONS_PATH)).andExpect(jsonPath("$.data.notifications.length()").value(0));
    }

    @Test
    void 운송장번호가_없거나_끝난_주문과_오래된_주문은_조회하지_않는다() throws Exception {
        createOrder(null);
        long deliveredOrderId = createOrder(TRACKING_NUMBER);
        long canceledOrderId = createOrder(TRACKING_NUMBER);
        long oldOrderId = createOrder(TRACKING_NUMBER);
        updateStatus(deliveredOrderId, "DELIVERED");
        updateStatus(canceledOrderId, "CANCELED");
        entityManager.flush();
        entityManager.clear();
        jdbcTemplate.update(
                "UPDATE orders SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(OLDER_THAN_MAX_ORDER_AGE)),
                oldOrderId
        );

        customsStatusScheduler.pollCustomsStatuses();

        verify(unipassClient, never()).getCargoStatus(any());
    }

    @Test
    void 운송장번호를_바꾸면_저장한_통관_상태를_지운다() throws Exception {
        long orderId = createOrder(TRACKING_NUMBER);
        givenCargoStatus(TRACKING_NUMBER, ARRIVED);
        customsStatusScheduler.pollCustomsStatuses();

        perform(put(ORDER_PATH, orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderBody(OTHER_TRACKING_NUMBER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customsStatus").doesNotExist())
                .andExpect(jsonPath("$.data.customsCheckedAt").doesNotExist());
    }

    @Test
    void 주문을_삭제하면_그_주문의_알림도_지워진다() throws Exception {
        long orderId = createOrder(TRACKING_NUMBER);
        givenCargoStatus(TRACKING_NUMBER, ARRIVED);
        customsStatusScheduler.pollCustomsStatuses();

        perform(delete(ORDER_PATH, orderId)).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        perform(get(NOTIFICATIONS_PATH))
                .andExpect(jsonPath("$.data.notifications.length()").value(0))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    private void givenCargoStatus(String trackingNumber, String progressStatus) {
        when(unipassClient.getCargoStatus(trackingNumber)).thenReturn(Optional.of(
                new CargoStatus("26POZ06265I00110321", "통관목록심사완료", progressStatus, FETCHED_AT, List.of(), FETCHED_AT)
        ));
    }

    private long createOrder(String internationalTrackingNumber) throws Exception {
        String response = perform(post(ORDERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderBody(internationalTrackingNumber))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updateStatus(long orderId, String status) throws Exception {
        perform(patch(ORDER_STATUS_PATH, orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(
                request.header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + accessToken)
        );
    }

    private Map<String, Object> orderBody(String internationalTrackingNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("productName", "빈티지 카메라");
        body.put("purchaseSource", "메루카리");
        body.put("purchaseDate", "2026-11-20");
        body.put("itemPriceJpy", 21000);
        body.put("localShippingFeeJpy", 800);
        body.put("internationalTrackingNumber", internationalTrackingNumber);
        return body;
    }
}
