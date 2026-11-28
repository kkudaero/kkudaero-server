package com.kkudaero.server.domain.notification.dto;

import java.util.List;

import com.kkudaero.server.domain.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

public record NotificationListResponse(
        @Schema(description = "알림 목록. 최근 알림이 먼저 옵니다.") List<NotificationResponse> notifications,
        @Schema(description = "읽지 않은 알림 수") long unreadCount,
        @Schema(description = "현재 페이지 번호. 0부터 시작합니다.") int page,
        @Schema(description = "다음 페이지가 있는지") boolean hasNext
) {

    public static NotificationListResponse of(Slice<Notification> notifications, long unreadCount) {
        return new NotificationListResponse(
                notifications.map(NotificationResponse::from).getContent(),
                unreadCount,
                notifications.getNumber(),
                notifications.hasNext()
        );
    }
}
