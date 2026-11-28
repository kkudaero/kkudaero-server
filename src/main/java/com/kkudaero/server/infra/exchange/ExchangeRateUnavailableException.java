package com.kkudaero.server.infra.exchange;

public final class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
