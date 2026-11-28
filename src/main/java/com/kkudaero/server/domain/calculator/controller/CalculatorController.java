package com.kkudaero.server.domain.calculator.controller;

import com.kkudaero.server.domain.calculator.dto.DutyEstimateRequest;
import com.kkudaero.server.domain.calculator.dto.DutyEstimateResponse;
import com.kkudaero.server.domain.calculator.service.CalculatorService;
import com.kkudaero.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/duty-estimates")
@RequiredArgsConstructor
@Tag(name = "Calculator", description = "관부가세·마진 예상 계산")
public class CalculatorController {

    private final CalculatorService calculatorService;

    @PostMapping
    @Operation(
            summary = "관부가세·마진 예상 계산",
            description = "목록통관 여부와 합산과세 위험, 예상 관부가세와 마진을 계산합니다. "
                    + "환율을 비우면 관세청 과세환율을 쓰고, 계산에 쓴 환율과 기준 시각을 응답에 담습니다. "
                    + "결과는 예상치이며 저장하지 않습니다."
    )
    public ApiResponse<DutyEstimateResponse> calculateDutyEstimate(@Valid @RequestBody DutyEstimateRequest request) {
        return ApiResponse.success(calculatorService.calculateDutyEstimate(request));
    }
}
