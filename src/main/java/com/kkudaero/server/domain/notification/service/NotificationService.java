package com.kkudaero.server.domain.notification.service;

import java.time.Clock;

import com.kkudaero.server.domain.notification.dto.NotificationListResponse;
import com.kkudaero.server.domain.notification.dto.NotificationResponse;
import com.kkudaero.server.domain.notification.entity.Notification;
import com.kkudaero.server.domain.notification.entity.NotificationType;
import com.kkudaero.server.domain.notification.error.NotificationErrorCode;
import com.kkudaero.server.domain.notification.repository.NotificationRepository;
import com.kkudaero.server.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional
    public void createNotification(Long sellerId, Long orderId, NotificationType type, String message) {
        notificationRepository.save(Notification.create(sellerId, orderId, type, message));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long sellerId, int page, int size) {
        return NotificationListResponse.of(
                notificationRepository.findBySellerId(sellerId, PageRequest.of(page, size, NEWEST_FIRST)),
                notificationRepository.countBySellerIdAndReadAtIsNull(sellerId)
        );
    }

    @Transactional
    public NotificationResponse updateNotificationRead(Long sellerId, Long notificationId, boolean isRead) {
        Notification notification = notificationRepository.findByIdAndSellerId(notificationId, sellerId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (isRead) {
            notification.markRead(clock.instant());
        } else {
            notification.markUnread();
        }
        return NotificationResponse.from(notification);
    }
}
