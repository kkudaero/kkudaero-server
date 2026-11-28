package com.kkudaero.server.domain.customs.service;

import com.kkudaero.server.domain.customs.dto.CustomsStatusResponse;
import com.kkudaero.server.domain.customs.error.CustomsErrorCode;
import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.error.OrderErrorCode;
import com.kkudaero.server.domain.order.repository.OrderRepository;
import com.kkudaero.server.global.error.BusinessException;
import com.kkudaero.server.infra.unipass.CargoStatus;
import com.kkudaero.server.infra.unipass.UnipassClient;
import com.kkudaero.server.infra.unipass.UnipassUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomsService {

    private final OrderRepository orderRepository;
    private final UnipassClient unipassClient;

    public CustomsStatusResponse getCustomsStatus(Long sellerId, Long orderId) {
        Order order = orderRepository.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        String trackingNumber = order.getInternationalTrackingNumber();
        if (trackingNumber == null) {
            throw new BusinessException(CustomsErrorCode.TRACKING_NUMBER_MISSING);
        }
        return CustomsStatusResponse.of(order, findCargoStatus(trackingNumber));
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
