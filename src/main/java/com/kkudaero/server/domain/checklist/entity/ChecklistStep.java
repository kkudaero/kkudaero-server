package com.kkudaero.server.domain.checklist.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChecklistStep {

    BUSINESS_REGISTRATION(1, "사업자등록"),
    MAIL_ORDER_REPORT(2, "통신판매업 신고"),
    CUSTOMS_ECOMMERCE_CODE(3, "관세청 전자상거래업자부호 발급"),
    SMARTSTORE_OVERSEAS_PERMISSION(4, "스마트스토어 해외상품 판매권한 신청"),
    FORWARDING_AGENT(5, "배대지 정하기");

    private final int order;
    private final String title;
}
