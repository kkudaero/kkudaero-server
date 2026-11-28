package com.kkudaero.server.domain.order.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.kkudaero.server.global.entity.BaseTimeEntity;
import com.kkudaero.server.global.util.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    public static final int PRODUCT_NAME_MAX_LENGTH = 200;
    public static final int PURCHASE_SOURCE_MAX_LENGTH = 100;
    public static final int RECIPIENT_NAME_MAX_LENGTH = 50;
    public static final int TRACKING_NUMBER_MAX_LENGTH = 100;
    public static final int MEMO_MAX_LENGTH = 1000;
    public static final int PRICE_INTEGER_DIGITS = 12;

    private static final int ENCRYPTED_COLUMN_LENGTH = 512;
    private static final int STATUS_MAX_LENGTH = 30;
    private static final int CUSTOMS_STATUS_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long sellerId;

    @Column(nullable = false, length = PRODUCT_NAME_MAX_LENGTH)
    private String productName;

    @Column(nullable = false, length = PURCHASE_SOURCE_MAX_LENGTH)
    private String purchaseSource;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false, precision = PRICE_INTEGER_DIGITS)
    private BigDecimal itemPriceJpy;

    @Column(nullable = false, precision = PRICE_INTEGER_DIGITS)
    private BigDecimal localShippingFeeJpy;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = ENCRYPTED_COLUMN_LENGTH)
    private String recipientName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = ENCRYPTED_COLUMN_LENGTH)
    private String personalCustomsCode;

    @Column(length = TRACKING_NUMBER_MAX_LENGTH)
    private String localTrackingNumber;

    @Column(length = TRACKING_NUMBER_MAX_LENGTH)
    private String internationalTrackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private OrderStatus status;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    @Column(length = CUSTOMS_STATUS_MAX_LENGTH)
    private String customsStatus;

    private Instant customsCheckedAt;

    private Order(Long sellerId, OrderDetails details) {
        this.sellerId = sellerId;
        this.status = OrderStatus.RECEIVED;
        update(details);
    }

    public static Order create(Long sellerId, OrderDetails details) {
        return new Order(sellerId, details);
    }

    public void update(OrderDetails details) {
        this.productName = details.productName();
        this.purchaseSource = details.purchaseSource();
        this.purchaseDate = details.purchaseDate();
        this.itemPriceJpy = details.itemPriceJpy();
        this.localShippingFeeJpy = details.localShippingFeeJpy();
        this.recipientName = details.recipientName();
        this.personalCustomsCode = details.personalCustomsCode();
        this.localTrackingNumber = details.localTrackingNumber();
        if (!Objects.equals(this.internationalTrackingNumber, details.internationalTrackingNumber())) {
            this.customsStatus = null;
            this.customsCheckedAt = null;
        }
        this.internationalTrackingNumber = details.internationalTrackingNumber();
        this.memo = details.memo();
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public boolean updateCustomsStatus(String customsStatus, Instant checkedAt) {
        boolean isChanged = !customsStatus.equals(this.customsStatus);
        this.customsStatus = customsStatus;
        this.customsCheckedAt = checkedAt;
        return isChanged;
    }
}
