package com.kkudaero.server.domain.seller.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.notification.entity.NotificationType;
import com.kkudaero.server.domain.notification.service.NotificationService;
import com.kkudaero.server.domain.seller.error.SellerErrorCode;
import com.kkudaero.server.global.error.GlobalErrorCode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerWithdrawalApiTest {

    private static final String SELLERS_PATH = "/api/v1/sellers";
    private static final String LOGIN_PATH = "/api/v1/sellers/login";
    private static final String ME_PATH = "/api/v1/sellers/me";
    private static final String WITHDRAWAL_PATH = "/api/v1/sellers/me/withdrawal";
    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String CHECKLIST_STEP_PATH = "/api/v1/checklist-steps/{step}";
    private static final String EMAIL = "seller@example.com";
    private static final List<String> SELLER_DATA_TABLES = List.of("orders", "notifications", "checklist_completions");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String accessToken;
    private long sellerId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, EMAIL);
        sellerId = SellerTestSupport.getSellerId(mockMvc, objectMapper, accessToken);
    }

    @Test
    void 탈퇴하면_계정과_주문_알림_체크리스트_기록이_모두_지워진다() throws Exception {
        createSellerData(accessToken, sellerId);

        withdraw(accessToken, SellerTestSupport.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        entityManager.flush();

        assertThat(countRows("sellers", "id", sellerId)).isZero();
        for (String table : SELLER_DATA_TABLES) {
            assertThat(countRows(table, "seller_id", sellerId)).as(table).isZero();
        }
    }

    @Test
    void 다른_셀러의_데이터는_지워지지_않는다() throws Exception {
        String otherAccessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "other@example.com");
        long otherSellerId = SellerTestSupport.getSellerId(mockMvc, objectMapper, otherAccessToken);
        createSellerData(accessToken, sellerId);
        createSellerData(otherAccessToken, otherSellerId);

        withdraw(accessToken, SellerTestSupport.PASSWORD).andExpect(status().isOk());
        entityManager.flush();

        assertThat(countRows("sellers", "id", otherSellerId)).isOne();
        for (String table : SELLER_DATA_TABLES) {
            assertThat(countRows(table, "seller_id", otherSellerId)).as(table).isOne();
        }
    }

    @Test
    void 비밀번호가_틀리면_403을_받고_아무것도_지워지지_않는다() throws Exception {
        createSellerData(accessToken, sellerId);

        withdraw(accessToken, SellerTestSupport.PASSWORD + "!")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(SellerErrorCode.SELLER_PASSWORD_MISMATCH.name()));
        entityManager.flush();

        assertThat(countRows("sellers", "id", sellerId)).isOne();
        assertThat(countRows("orders", "seller_id", sellerId)).isOne();
    }

    @Test
    void 탈퇴한_뒤에는_이전_토큰과_로그인이_모두_401을_받는다() throws Exception {
        withdraw(accessToken, SellerTestSupport.PASSWORD).andExpect(status().isOk());
        entityManager.flush();

        authorized(get(ME_PATH), accessToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
        authorized(get(ORDERS_PATH), accessToken).andExpect(status().isUnauthorized());
        mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SellerErrorCode.SELLER_LOGIN_FAILED.name()));
    }

    @Test
    void 탈퇴한_이메일로_다시_가입할_수_있다() throws Exception {
        withdraw(accessToken, SellerTestSupport.PASSWORD).andExpect(status().isOk());
        entityManager.flush();

        mockMvc.perform(post(SELLERS_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    @Test
    void 비밀번호_없이_탈퇴하거나_토큰_없이_탈퇴할_수_없다() throws Exception {
        authorized(post(WITHDRAWAL_PATH).contentType(MediaType.APPLICATION_JSON).content("{}"), accessToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
        mockMvc.perform(post(WITHDRAWAL_PATH).contentType(MediaType.APPLICATION_JSON).content(credentials()))
                .andExpect(status().isUnauthorized());
    }

    private void createSellerData(String token, long ownerId) throws Exception {
        Map<String, Object> order = Map.of(
                "productName", "빈티지 카메라",
                "purchaseSource", "메루카리",
                "purchaseDate", "2026-11-20",
                "itemPriceJpy", 21000,
                "localShippingFeeJpy", 800,
                "recipientName", "홍길동",
                "personalCustomsCode", "P123456789012"
        );
        String response = authorized(
                post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)),
                token
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(response).at("/data/id").asLong();
        notificationService.createNotification(ownerId, orderId, NotificationType.CUSTOMS_STATUS_CHANGED, "알림");
        authorized(
                patch(CHECKLIST_STEP_PATH, "BUSINESS_REGISTRATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isCompleted", true))),
                token
        ).andExpect(status().isOk());
    }

    private ResultActions withdraw(String token, String password) throws Exception {
        return authorized(
                post(WITHDRAWAL_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", password))),
                token
        );
    }

    private ResultActions authorized(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token));
    }

    private String credentials() throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", SellerTestSupport.PASSWORD));
    }

    private int countRows(String table, String column, long value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }
}
