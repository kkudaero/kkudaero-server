package com.kkudaero.server.domain.order.entity;

public enum OrderStatus {

    RECEIVED,
    PURCHASED,
    WAREHOUSE_ARRIVED,
    IN_INTERNATIONAL_TRANSIT,
    IN_CUSTOMS,
    CUSTOMS_HELD,
    IN_DOMESTIC_TRANSIT,
    DELIVERED,
    CANCELED
}
