package com.kkudaero.server.domain.customs.dto;

import java.time.Instant;
import java.util.List;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.infra.unipass.CargoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomsStatusResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "조회에 쓴 국제 운송장번호") String internationalTrackingNumber,
        @Schema(description = "관세청 화물관리번호") String cargoManagementNumber,
        @Schema(description = "통관 진행 상태. 관세청이 준 값 그대로입니다.", example = "통관목록심사완료")
        String clearanceStatus,
        @Schema(description = "화물 진행 상태. 관세청이 준 값 그대로입니다.", example = "반출완료")
        String progressStatus,
        @Schema(description = "마지막 처리 시각(UTC)", nullable = true) Instant processedAt,
        @Schema(description = "단계별 이력") List<CustomsEventResponse> events,
        @Schema(description = "관세청에서 조회한 시각(UTC). 캐시된 결과면 과거 시각입니다.") Instant fetchedAt
) {

    public static CustomsStatusResponse of(Order order, CargoStatus cargoStatus) {
        return new CustomsStatusResponse(
                order.getId(),
                order.getInternationalTrackingNumber(),
                cargoStatus.cargoManagementNumber(),
                cargoStatus.clearanceStatus(),
                cargoStatus.progressStatus(),
                cargoStatus.processedAt(),
                cargoStatus.events().stream().map(CustomsEventResponse::from).toList(),
                cargoStatus.fetchedAt()
        );
    }
}
