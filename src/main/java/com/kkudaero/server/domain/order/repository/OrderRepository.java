package com.kkudaero.server.domain.order.repository;

import java.util.Optional;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndSellerId(Long id, Long sellerId);

    Slice<Order> findBySellerId(Long sellerId, Pageable pageable);

    Slice<Order> findBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable);
}
