package com.kkudaero.server.domain.checklist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChecklistStepRequest(
        @Schema(description = "완료했으면 true, 되돌리면 false") @NotNull Boolean isCompleted
) {
}
