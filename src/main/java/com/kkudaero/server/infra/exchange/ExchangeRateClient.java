package com.kkudaero.server.infra.exchange;

public interface ExchangeRateClient {

    ExchangeRate getImportRate(CurrencyCode currency);
}
