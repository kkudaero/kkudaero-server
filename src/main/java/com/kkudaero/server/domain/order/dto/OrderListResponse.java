package com.kkudaero.server.domain.order.dto;

import java.util.List;

import com.kkudaero.server.domain.order.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

public record OrderListResponse(
        @Schema(description = "주문 목록. 최근에 등록한 주문이 먼저 옵니다.") List<OrderSummaryResponse> orders,
        @Schema(description = "현재 페이지 번호. 0부터 시작합니다.") int page,
        @Schema(description = "다음 페이지가 있는지") boolean hasNext
) {

    public static OrderListResponse from(Slice<Order> orders) {
        return new OrderListResponse(
                orders.map(OrderSummaryResponse::from).getContent(),
                orders.getNumber(),
                orders.hasNext()
        );
    }
}
