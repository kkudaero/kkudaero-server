package com.kkudaero.server.domain.calculator.service;

import java.math.BigDecimal;

public final class DutyPolicy {

    public static final BigDecimal LIST_CLEARANCE_LIMIT_USD = new BigDecimal("150");
    public static final BigDecimal VAT_RATE_PERCENT = new BigDecimal("10");

    private DutyPolicy() {
    }
}
