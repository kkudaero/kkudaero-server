package com.kkudaero.server.domain.checklist.controller;

import java.util.List;

import com.kkudaero.server.domain.checklist.dto.ChecklistStepRequest;
import com.kkudaero.server.domain.checklist.dto.ChecklistStepResponse;
import com.kkudaero.server.domain.checklist.entity.ChecklistStep;
import com.kkudaero.server.domain.checklist.service.ChecklistService;
import com.kkudaero.server.global.response.ApiResponse;
import com.kkudaero.server.global.security.SellerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checklist-steps")
@RequiredArgsConstructor
@Tag(name = "Checklist", description = "구매대행 시작 체크리스트")
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    @Operation(summary = "시작 체크리스트 조회", description = "시작 준비 단계를 순서대로 돌려주고 내가 완료한 단계를 표시합니다.")
    public ApiResponse<List<ChecklistStepResponse>> getChecklistSteps(
            @AuthenticationPrincipal SellerPrincipal principal
    ) {
        return ApiResponse.success(checklistService.getChecklistSteps(principal.sellerId()));
    }

    @PatchMapping("/{step}")
    @Operation(
            summary = "체크리스트 단계 완료 표시",
            description = "단계를 완료로 표시하거나 되돌립니다. 순서와 상관없이 표시할 수 있고, 같은 요청을 다시 보내도 결과가 같습니다."
    )
    public ApiResponse<ChecklistStepResponse> updateChecklistStep(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable ChecklistStep step,
            @Valid @RequestBody ChecklistStepRequest request
    ) {
        return ApiResponse.success(
                checklistService.updateChecklistStep(principal.sellerId(), step, request.isCompleted())
        );
    }
}
