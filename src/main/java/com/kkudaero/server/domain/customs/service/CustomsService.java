package com.kkudaero.server.domain.customs.service;

import com.kkudaero.server.domain.customs.dto.CustomsStatusResponse;
import com.kkudaero.server.domain.customs.error.CustomsErrorCode;
import com.kkudaero.server.domain.notification.entity.NotificationType;
import com.kkudaero.server.domain.notification.service.NotificationService;
import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.error.OrderErrorCode;
import com.kkudaero.server.domain.order.repository.OrderRepository;
import com.kkudaero.server.global.error.BusinessException;
import com.kkudaero.server.infra.unipass.CargoStatus;
import com.kkudaero.server.infra.unipass.UnipassClient;
import com.kkudaero.server.infra.unipass.UnipassUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomsService {

    private static final String STATUS_CHANGED_MESSAGE_FORMAT = "'%s' 주문의 통관 상태가 '%s'(으)로 바뀌었습니다.";

    private final OrderRepository orderRepository;
    private final UnipassClient unipassClient;
    private final NotificationService notificationService;

    public CustomsStatusResponse getCustomsStatus(Long sellerId, Long orderId) {
        Order order = orderRepository.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        String trackingNumber = order.getInternationalTrackingNumber();
        if (trackingNumber == null) {
            throw new BusinessException(CustomsErrorCode.TRACKING_NUMBER_MISSING);
        }
        return CustomsStatusResponse.of(order, findCargoStatus(trackingNumber));
    }

    @Transactional
    public void recordCustomsStatus(Long orderId, CargoStatus cargoStatus) {
        String customsStatus = StringUtils.hasText(cargoStatus.progressStatus())
                ? cargoStatus.progressStatus()
                : cargoStatus.clearanceStatus();
        if (!StringUtils.hasText(customsStatus)) {
            return;
        }

        orderRepository.findById(orderId)
                .filter(order -> order.updateCustomsStatus(customsStatus, cargoStatus.fetchedAt()))
                .ifPresent(order -> notificationService.createNotification(
                        order.getSellerId(),
                        order.getId(),
                        NotificationType.CUSTOMS_STATUS_CHANGED,
                        STATUS_CHANGED_MESSAGE_FORMAT.formatted(order.getProductName(), customsStatus)
                ));
    }

    private CargoStatus findCargoStatus(String trackingNumber) {
        try {
            return unipassClient.getCargoStatus(trackingNumber)
                    .orElseThrow(() -> new BusinessException(CustomsErrorCode.CUSTOMS_STATUS_NOT_FOUND));
        } catch (UnipassUnavailableException exception) {
            throw new BusinessException(CustomsErrorCode.CUSTOMS_STATUS_UNAVAILABLE);
        }
    }
}
