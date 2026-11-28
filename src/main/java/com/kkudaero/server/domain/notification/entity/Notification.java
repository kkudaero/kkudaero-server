package com.kkudaero.server.domain.notification.entity;

import java.time.Instant;

import com.kkudaero.server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    private static final int TYPE_MAX_LENGTH = 50;
    private static final int MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long sellerId;

    @Column(updatable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = TYPE_MAX_LENGTH)
    private NotificationType type;

    @Column(nullable = false, updatable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    private Instant readAt;

    private Notification(Long sellerId, Long orderId, NotificationType type, String message) {
        this.sellerId = sellerId;
        this.orderId = orderId;
        this.type = type;
        this.message = message;
    }

    public static Notification create(Long sellerId, Long orderId, NotificationType type, String message) {
        return new Notification(sellerId, orderId, type, message);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public void markUnread() {
        readAt = null;
    }
}
