package com.kkudaero.server.domain.notification.dto;

import java.time.Instant;

import com.kkudaero.server.domain.notification.entity.Notification;
import com.kkudaero.server.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationResponse(
        @Schema(description = "알림 ID") Long id,
        @Schema(description = "알림 종류") NotificationType type,
        @Schema(description = "관련 주문 ID", nullable = true) Long orderId,
        @Schema(description = "알림 문구") String message,
        @Schema(description = "읽었는지") boolean isRead,
        @Schema(description = "읽은 시각(UTC)", nullable = true) Instant readAt,
        @Schema(description = "알림이 만들어진 시각(UTC)") Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getOrderId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
