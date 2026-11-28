package com.kkudaero.server.domain.notification.repository;

import java.util.Optional;

import com.kkudaero.server.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndSellerId(Long id, Long sellerId);

    Slice<Notification> findBySellerId(Long sellerId, Pageable pageable);

    long countBySellerIdAndReadAtIsNull(Long sellerId);
}
