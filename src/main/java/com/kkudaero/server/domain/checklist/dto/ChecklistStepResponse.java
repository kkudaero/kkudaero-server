package com.kkudaero.server.domain.checklist.dto;

import java.time.Instant;

import com.kkudaero.server.domain.checklist.entity.ChecklistCompletion;
import com.kkudaero.server.domain.checklist.entity.ChecklistStep;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChecklistStepResponse(
        @Schema(description = "단계") ChecklistStep step,
        @Schema(description = "단계 순서. 1부터 시작합니다.") int order,
        @Schema(description = "단계 이름", example = "사업자등록") String title,
        @Schema(description = "완료했는지") boolean isCompleted,
        @Schema(description = "완료로 표시한 시각(UTC). 완료하지 않았으면 null", nullable = true) Instant completedAt
) {

    public static ChecklistStepResponse completed(ChecklistCompletion completion) {
        ChecklistStep step = completion.getStep();
        return new ChecklistStepResponse(step, step.getOrder(), step.getTitle(), true, completion.getCreatedAt());
    }

    public static ChecklistStepResponse notCompleted(ChecklistStep step) {
        return new ChecklistStepResponse(step, step.getOrder(), step.getTitle(), false, null);
    }
}
