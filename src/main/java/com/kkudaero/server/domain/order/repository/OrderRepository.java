package com.kkudaero.server.domain.order.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndSellerId(Long id, Long sellerId);

    Slice<Order> findBySellerId(Long sellerId, Pageable pageable);

    Slice<Order> findBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable);

    @Query("""
            select o from Order o
            where o.internationalTrackingNumber is not null
              and o.status not in :finishedStatuses
              and o.createdAt >= :createdFrom
            order by o.id
            """)
    List<Order> findCustomsPollTargets(
            @Param("finishedStatuses") Collection<OrderStatus> finishedStatuses,
            @Param("createdFrom") Instant createdFrom
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Order e where e.sellerId = :sellerId")
    void deleteAllBySellerId(@Param("sellerId") Long sellerId);
}
