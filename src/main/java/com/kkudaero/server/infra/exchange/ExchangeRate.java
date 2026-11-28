package com.kkudaero.server.infra.exchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExchangeRate(CurrencyCode currency, BigDecimal krwPerUnit, LocalDate appliedFrom, Instant fetchedAt) {
}
