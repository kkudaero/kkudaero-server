package com.kkudaero.server.domain.order.service;

import com.kkudaero.server.domain.order.dto.OrderListResponse;
import com.kkudaero.server.domain.order.dto.OrderRequest;
import com.kkudaero.server.domain.order.dto.OrderResponse;
import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import com.kkudaero.server.domain.order.error.OrderErrorCode;
import com.kkudaero.server.domain.order.repository.OrderRepository;
import com.kkudaero.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(Long sellerId, OrderRequest request) {
        Order order = Order.create(sellerId, request.toDetails());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderListResponse getOrders(Long sellerId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Slice<Order> orders = status == null
                ? orderRepository.findBySellerId(sellerId, pageable)
                : orderRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        return OrderListResponse.from(orders);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long sellerId, Long orderId) {
        return OrderResponse.from(findOwnOrder(sellerId, orderId));
    }

    @Transactional
    public OrderResponse updateOrder(Long sellerId, Long orderId, OrderRequest request) {
        Order order = findOwnOrder(sellerId, orderId);
        order.update(request.toDetails());
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long sellerId, Long orderId, OrderStatus status) {
        Order order = findOwnOrder(sellerId, orderId);
        order.updateStatus(status);
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public void deleteOrder(Long sellerId, Long orderId) {
        orderRepository.delete(findOwnOrder(sellerId, orderId));
    }

    private Order findOwnOrder(Long sellerId, Long orderId) {
        return orderRepository.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
