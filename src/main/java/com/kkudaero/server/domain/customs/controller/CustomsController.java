package com.kkudaero.server.domain.customs.controller;

import com.kkudaero.server.domain.customs.dto.CustomsStatusResponse;
import com.kkudaero.server.domain.customs.service.CustomsService;
import com.kkudaero.server.global.response.ApiResponse;
import com.kkudaero.server.global.security.SellerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/customs-status")
@RequiredArgsConstructor
@Tag(name = "Customs", description = "통관 진행 조회")
public class CustomsController {

    private final CustomsService customsService;

    @GetMapping
    @Operation(
            summary = "주문의 통관 진행 조회",
            description = "주문의 국제 운송장번호로 관세청 화물통관 진행정보를 조회합니다. "
                    + "운송장번호가 없으면 409, 관세청에 아직 정보가 없으면 404, 조회에 실패하면 503을 돌려줍니다. "
                    + "결과는 잠시 캐시되며 조회한 시각을 함께 줍니다."
    )
    public ApiResponse<CustomsStatusResponse> getCustomsStatus(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(customsService.getCustomsStatus(principal.sellerId(), orderId));
    }
}
