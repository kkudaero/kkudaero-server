package com.kkudaero.server.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.order.error.OrderErrorCode;
import com.kkudaero.server.global.error.GlobalErrorCode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderApiTest {

    private static final String SELLERS_PATH = "/api/v1/sellers";
    private static final String LOGIN_PATH = "/api/v1/sellers/login";
    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String ORDER_PATH = "/api/v1/orders/{orderId}";
    private static final String ORDER_STATUS_PATH = "/api/v1/orders/{orderId}/status";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PASSWORD = "password-1234";
    private static final String RECIPIENT_NAME = "홍길동";
    private static final String PERSONAL_CUSTOMS_CODE = "P123456789012";
    private static final String MASKED_PERSONAL_CUSTOMS_CODE = "P**********12";
    private static final String ENCRYPTED_PREFIX = "v1:";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String accessToken;
    private String otherAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = signUpAndLogin("seller@example.com");
        otherAccessToken = signUpAndLogin("other@example.com");
    }

    @Test
    void 주문을_등록하면_주문_접수_상태로_저장된다() throws Exception {
        createOrder(accessToken, validBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.productName").value("빈티지 카메라"))
                .andExpect(jsonPath("$.data.itemPriceJpy").value(21000))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.personalCustomsCode").value(PERSONAL_CUSTOMS_CODE))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void 개인정보는_암호문으로_저장되고_조회하면_원래_값으로_돌아온다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());
        entityManager.flush();
        entityManager.clear();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT recipient_name, personal_customs_code FROM orders WHERE id = ?", orderId);
        assertThat(row.get("recipient_name").toString())
                .startsWith(ENCRYPTED_PREFIX)
                .doesNotContain(RECIPIENT_NAME);
        assertThat(row.get("personal_customs_code").toString())
                .startsWith(ENCRYPTED_PREFIX)
                .doesNotContain(PERSONAL_CUSTOMS_CODE);

        perform(get(ORDER_PATH, orderId), accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value(RECIPIENT_NAME))
                .andExpect(jsonPath("$.data.personalCustomsCode").value(PERSONAL_CUSTOMS_CODE));
    }

    @Test
    void 선택값_없이도_주문을_등록할_수_있다() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("recipientName");
        body.remove("personalCustomsCode");
        body.put("memo", "   ");

        createOrder(accessToken, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recipientName").doesNotExist())
                .andExpect(jsonPath("$.data.personalCustomsCode").doesNotExist())
                .andExpect(jsonPath("$.data.memo").doesNotExist());
    }

    @Test
    void 목록은_내_주문만_최근_순으로_주고_개인통관고유부호를_가린다() throws Exception {
        Map<String, Object> second = validBody();
        second.put("productName", "필름");
        createOrderAndGetId(accessToken, validBody());
        long secondOrderId = createOrderAndGetId(accessToken, second);
        createOrderAndGetId(otherAccessToken, validBody());

        perform(get(ORDERS_PATH), accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders.length()").value(2))
                .andExpect(jsonPath("$.data.orders[0].id").value(secondOrderId))
                .andExpect(jsonPath("$.data.orders[0].maskedPersonalCustomsCode").value(MASKED_PERSONAL_CUSTOMS_CODE))
                .andExpect(jsonPath("$.data.orders[0].personalCustomsCode").doesNotExist())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void 목록을_상태로_거르고_페이지로_나눈다() throws Exception {
        long firstOrderId = createOrderAndGetId(accessToken, validBody());
        createOrderAndGetId(accessToken, validBody());
        updateStatus(accessToken, firstOrderId, "IN_CUSTOMS").andExpect(status().isOk());

        perform(get(ORDERS_PATH).param("status", "IN_CUSTOMS"), accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders.length()").value(1))
                .andExpect(jsonPath("$.data.orders[0].id").value(firstOrderId));

        perform(get(ORDERS_PATH).param("size", "1"), accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void 주문을_수정하면_비운_선택값은_지워진다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());
        Map<String, Object> body = validBody();
        body.put("productName", "렌즈");
        body.put("internationalTrackingNumber", "EJ123456789JP");
        body.remove("recipientName");

        perform(put(ORDER_PATH, orderId), accessToken, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("렌즈"))
                .andExpect(jsonPath("$.data.internationalTrackingNumber").value("EJ123456789JP"))
                .andExpect(jsonPath("$.data.recipientName").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void 상태는_순서와_상관없이_바꿀_수_있다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());

        updateStatus(accessToken, orderId, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
        updateStatus(accessToken, orderId, "CUSTOMS_HELD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CUSTOMS_HELD"));
    }

    @Test
    void 주문을_삭제하면_다시_조회할_수_없다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());

        perform(delete(ORDER_PATH, orderId), accessToken).andExpect(status().isOk());

        perform(get(ORDER_PATH, orderId), accessToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(OrderErrorCode.ORDER_NOT_FOUND.name()));
    }

    @Test
    void 다른_셀러의_주문은_조회_수정_상태_변경_삭제_모두_404를_받는다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());

        perform(get(ORDER_PATH, orderId), otherAccessToken).andExpect(status().isNotFound());
        perform(put(ORDER_PATH, orderId), otherAccessToken, validBody()).andExpect(status().isNotFound());
        updateStatus(otherAccessToken, orderId, "CANCELED").andExpect(status().isNotFound());
        perform(delete(ORDER_PATH, orderId), otherAccessToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(OrderErrorCode.ORDER_NOT_FOUND.name()));

        perform(get(ORDER_PATH, orderId), accessToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    void 개인통관고유부호_형식이_틀리면_400을_받는다() throws Exception {
        Map<String, Object> body = validBody();
        body.put("personalCustomsCode", "P12345");

        createOrder(accessToken, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 필수_값이_빠지거나_금액이_음수면_400을_받는다() throws Exception {
        Map<String, Object> withoutProductName = validBody();
        withoutProductName.put("productName", " ");
        Map<String, Object> negativePrice = validBody();
        negativePrice.put("itemPriceJpy", -1);

        createOrder(accessToken, withoutProductName).andExpect(status().isBadRequest());
        createOrder(accessToken, negativePrice)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 없는_상태나_범위를_벗어난_페이지_크기는_400을_받는다() throws Exception {
        long orderId = createOrderAndGetId(accessToken, validBody());

        updateStatus(accessToken, orderId, "UNKNOWN").andExpect(status().isBadRequest());
        perform(get(ORDERS_PATH).param("status", "UNKNOWN"), accessToken).andExpect(status().isBadRequest());
        perform(get(ORDERS_PATH).param("size", "101"), accessToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 토큰_없이_주문을_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(ORDERS_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    private String signUpAndLogin(String email) throws Exception {
        String credentials = objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        mockMvc.perform(post(SELLERS_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String loginBody = mockMvc.perform(
                        post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginBody).at("/data/accessToken").asText();
    }

    private ResultActions createOrder(String token, Map<String, Object> body) throws Exception {
        return perform(post(ORDERS_PATH), token, body);
    }

    private long createOrderAndGetId(String token, Map<String, Object> body) throws Exception {
        String response = createOrder(token, body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private ResultActions updateStatus(String token, long orderId, String status) throws Exception {
        return perform(patch(ORDER_STATUS_PATH, orderId), token, Map.of("status", status));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token));
    }

    private ResultActions perform(
            MockHttpServletRequestBuilder request,
            String token,
            Map<String, Object> body
    ) throws Exception {
        return perform(
                request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)),
                token
        );
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("productName", "빈티지 카메라");
        body.put("purchaseSource", "메루카리");
        body.put("purchaseDate", "2026-11-28");
        body.put("itemPriceJpy", 21000);
        body.put("localShippingFeeJpy", 800);
        body.put("recipientName", RECIPIENT_NAME);
        body.put("personalCustomsCode", PERSONAL_CUSTOMS_CODE);
        body.put("localTrackingNumber", "1234-5678-9012");
        body.put("memo", "박스 포함");
        return body;
    }
}
