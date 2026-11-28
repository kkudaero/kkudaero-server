package com.kkudaero.server.domain.notification.controller;

import com.kkudaero.server.domain.notification.dto.NotificationListResponse;
import com.kkudaero.server.domain.notification.dto.NotificationReadRequest;
import com.kkudaero.server.domain.notification.dto.NotificationResponse;
import com.kkudaero.server.domain.notification.service.NotificationService;
import com.kkudaero.server.global.response.ApiResponse;
import com.kkudaero.server.global.security.SellerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림")
public class NotificationController {

    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "내 알림을 최근 순으로 돌려주고 읽지 않은 알림 수를 함께 줍니다.")
    public ApiResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal SellerPrincipal principal,
            @Parameter(description = "페이지 번호. 0부터 시작합니다.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "한 페이지의 알림 수. 최대 100")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
    ) {
        return ApiResponse.success(notificationService.getNotifications(principal.sellerId(), page, size));
    }

    @PatchMapping("/{notificationId}")
    @Operation(summary = "알림 읽음 표시", description = "알림을 읽음으로 표시하거나 되돌립니다. 내 알림이 아니면 404를 돌려줍니다.")
    public ApiResponse<NotificationResponse> updateNotificationRead(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationReadRequest request
    ) {
        return ApiResponse.success(
                notificationService.updateNotificationRead(principal.sellerId(), notificationId, request.isRead())
        );
    }
}
