package com.kkudaero.server.domain.customs.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.kkudaero.server.domain.order.entity.Order;
import com.kkudaero.server.domain.order.entity.OrderStatus;
import com.kkudaero.server.domain.order.repository.OrderRepository;
import com.kkudaero.server.infra.unipass.UnipassClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomsStatusScheduler {

    private static final Set<OrderStatus> FINISHED_STATUSES = Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELED);

    private final OrderRepository orderRepository;
    private final UnipassClient unipassClient;
    private final CustomsService customsService;
    private final CustomsPollProperties properties;
    private final Clock clock;

    @Scheduled(
            fixedDelayString = "${kkudaero.customs.poll-interval}",
            initialDelayString = "${kkudaero.customs.poll-initial-delay}"
    )
    public void pollCustomsStatuses() {
        Instant createdFrom = clock.instant().minus(properties.pollMaxOrderAge());
        List<Order> orders = orderRepository.findCustomsPollTargets(FINISHED_STATUSES, createdFrom);
        int failedCount = 0;
        for (Order order : orders) {
            try {
                unipassClient.getCargoStatus(order.getInternationalTrackingNumber())
                        .ifPresent(cargoStatus -> customsService.recordCustomsStatus(order.getId(), cargoStatus));
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn(
                        "주문 {}의 통관 상태 조회가 실패했습니다: {}",
                        order.getId(),
                        exception.getClass().getSimpleName()
                );
            }
        }
        log.info("통관 상태 자동 조회를 마쳤습니다. 대상 {}건, 실패 {}건", orders.size(), failedCount);
    }
}
