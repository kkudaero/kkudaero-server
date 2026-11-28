package com.kkudaero.server.domain.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkudaero.server.domain.notification.entity.NotificationType;
import com.kkudaero.server.domain.notification.error.NotificationErrorCode;
import com.kkudaero.server.domain.notification.service.NotificationService;
import com.kkudaero.server.global.error.GlobalErrorCode;
import com.kkudaero.server.support.SellerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationApiTest {

    private static final String NOTIFICATIONS_PATH = "/api/v1/notifications";
    private static final String NOTIFICATION_PATH = "/api/v1/notifications/{notificationId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    private String accessToken;
    private long sellerId;

    @BeforeEach
    void setUp() throws Exception {
        accessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "seller@example.com");
        sellerId = SellerTestSupport.getSellerId(mockMvc, objectMapper, accessToken);
    }

    @Test
    void 알림은_최근_순으로_나오고_페이지로_나눈다() throws Exception {
        createNotification("첫 번째 알림");
        createNotification("두 번째 알림");

        getNotifications(accessToken, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].message").value("두 번째 알림"))
                .andExpect(jsonPath("$.data.unreadCount").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void 알림을_읽음으로_표시하고_되돌릴_수_있다() throws Exception {
        createNotification("알림");
        long notificationId = firstNotificationId(accessToken);

        updateRead(accessToken, notificationId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").exists());
        getNotifications(accessToken, "20").andExpect(jsonPath("$.data.unreadCount").value(0));

        updateRead(accessToken, notificationId, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.readAt").doesNotExist());
        getNotifications(accessToken, "20").andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void 다른_셀러의_알림은_보이지_않고_읽음_표시하면_404를_받는다() throws Exception {
        createNotification("알림");
        long notificationId = firstNotificationId(accessToken);
        String otherAccessToken = SellerTestSupport.signUpAndLogin(mockMvc, objectMapper, "other@example.com");

        getNotifications(otherAccessToken, "20")
                .andExpect(jsonPath("$.data.notifications.length()").value(0))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
        updateRead(otherAccessToken, notificationId, true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(NotificationErrorCode.NOTIFICATION_NOT_FOUND.name()));
        getNotifications(accessToken, "20").andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void 빈_요청이나_범위를_벗어난_페이지_크기는_400을_받는다() throws Exception {
        createNotification("알림");
        long notificationId = firstNotificationId(accessToken);

        mockMvc.perform(patch(NOTIFICATION_PATH, notificationId)
                        .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        getNotifications(accessToken, "101")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_REQUEST.name()));
    }

    @Test
    void 토큰_없이_조회하면_401을_받는다() throws Exception {
        mockMvc.perform(get(NOTIFICATIONS_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.name()));
    }

    private void createNotification(String message) {
        notificationService.createNotification(sellerId, null, NotificationType.CUSTOMS_STATUS_CHANGED, message);
    }

    private long firstNotificationId(String token) throws Exception {
        String body = getNotifications(token, "20").andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/notifications/0/id").asLong();
    }

    private ResultActions getNotifications(String token, String size) throws Exception {
        return mockMvc.perform(get(NOTIFICATIONS_PATH)
                .param("size", size)
                .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token));
    }

    private ResultActions updateRead(String token, long notificationId, boolean isRead) throws Exception {
        return mockMvc.perform(patch(NOTIFICATION_PATH, notificationId)
                .header(HttpHeaders.AUTHORIZATION, SellerTestSupport.BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("isRead", isRead))));
    }
}
