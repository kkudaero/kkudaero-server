package com.kkudaero.server.domain.order.controller;

import com.kkudaero.server.domain.order.dto.OrderListResponse;
import com.kkudaero.server.domain.order.dto.OrderRequest;
import com.kkudaero.server.domain.order.dto.OrderResponse;
import com.kkudaero.server.domain.order.dto.OrderStatusRequest;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import com.kkudaero.server.domain.order.service.OrderService;
import com.kkudaero.server.global.response.ApiResponse;
import com.kkudaero.server.global.security.SellerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "수동 입력 주문 추적 보드")
public class OrderController {

    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "주문 등록", description = "주문을 등록합니다. 상태는 주문 접수(RECEIVED)로 시작합니다.")
    public ApiResponse<OrderResponse> createOrder(
            @AuthenticationPrincipal SellerPrincipal principal,
            @Valid @RequestBody OrderRequest request
    ) {
        return ApiResponse.success(orderService.createOrder(principal.sellerId(), request));
    }

    @GetMapping
    @Operation(
            summary = "내 주문 목록 조회",
            description = "내 주문을 최근에 등록한 순서로 돌려줍니다. 개인통관고유부호는 가려서 줍니다."
    )
    public ApiResponse<OrderListResponse> getOrders(
            @AuthenticationPrincipal SellerPrincipal principal,
            @Parameter(description = "이 상태의 주문만 조회합니다. 비우면 전체를 조회합니다.")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "페이지 번호. 0부터 시작합니다.")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "한 페이지의 주문 수. 최대 100")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
    ) {
        return ApiResponse.success(orderService.getOrders(principal.sellerId(), status, page, size));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "내 주문이 아니면 404를 돌려줍니다.")
    public ApiResponse<OrderResponse> getOrder(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.getOrder(principal.sellerId(), orderId));
    }

    @PutMapping("/{orderId}")
    @Operation(
            summary = "주문 수정",
            description = "상태를 뺀 주문 내용을 요청 값으로 모두 바꿉니다. 비운 선택값은 지워집니다."
    )
    public ApiResponse<OrderResponse> updateOrder(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderRequest request
    ) {
        return ApiResponse.success(orderService.updateOrder(principal.sellerId(), orderId, request));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "주문 상태 변경", description = "순서와 상관없이 어느 상태로든 바꿀 수 있습니다.")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusRequest request
    ) {
        return ApiResponse.success(
                orderService.updateOrderStatus(principal.sellerId(), orderId, request.status())
        );
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "주문 삭제", description = "주문과 함께 저장한 개인정보를 지웁니다. 되돌릴 수 없습니다.")
    public ApiResponse<Void> deleteOrder(
            @AuthenticationPrincipal SellerPrincipal principal,
            @PathVariable Long orderId
    ) {
        orderService.deleteOrder(principal.sellerId(), orderId);
        return ApiResponse.success();
    }
}
