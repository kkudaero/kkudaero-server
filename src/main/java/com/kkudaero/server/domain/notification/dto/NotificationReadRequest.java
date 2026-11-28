package com.kkudaero.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record NotificationReadRequest(
        @Schema(description = "읽었으면 true, 읽지 않음으로 되돌리면 false") @NotNull Boolean isRead
) {
}
