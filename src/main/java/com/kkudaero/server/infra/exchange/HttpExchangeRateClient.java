package com.kkudaero.server.infra.exchange;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpExchangeRateClient implements ExchangeRateClient {

    private static final ZoneId CUSTOMS_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String IMPORT_RATE_TYPE = "2";
    private static final String SERVICE_KEY_PARAM = "serviceKey";
    private static final String DATE_PARAM = "aplyBgnDt";
    private static final String RATE_TYPE_PARAM = "weekFxrtTpcd";

    private final RestClient exchangeRestClient;
    private final ExchangeProperties properties;
    private final Clock clock;

    private Snapshot snapshot;

    @Override
    public synchronized ExchangeRate getImportRate(CurrencyCode currency) {
        LocalDate today = LocalDate.now(clock.withZone(CUSTOMS_ZONE));
        if (snapshot == null || !snapshot.queriedOn().equals(today)) {
            snapshot = new Snapshot(today, fetchRates(today));
        }

        ExchangeRate rate = snapshot.rates().get(currency);
        if (rate == null) {
            throw new ExchangeRateUnavailableException("응답에 " + currency + " 환율이 없습니다.");
        }
        return rate;
    }

    private Map<CurrencyCode, ExchangeRate> fetchRates(LocalDate date) {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new ExchangeRateUnavailableException("환율 조회 인증키가 설정되지 않았습니다.");
        }

        CustomsExchangeRatePayload payload = CustomsExchangeRatePayload.parse(requestRates(date));
        if (!payload.isSuccessful()) {
            throw new ExchangeRateUnavailableException("환율 조회가 실패했습니다. 결과 코드: " + payload.resultCode());
        }
        return toRates(payload, clock.instant());
    }

    private String requestRates(LocalDate date) {
        URI uri = UriComponentsBuilder.fromUriString(properties.url())
                .queryParam(SERVICE_KEY_PARAM, "{serviceKey}")
                .queryParam(DATE_PARAM, date.format(DATE_FORMAT))
                .queryParam(RATE_TYPE_PARAM, IMPORT_RATE_TYPE)
                .encode()
                .buildAndExpand(properties.serviceKey())
                .toUri();
        try {
            String body = exchangeRestClient.get().uri(uri).retrieve().body(String.class);
            if (!StringUtils.hasText(body)) {
                throw new ExchangeRateUnavailableException("환율 응답이 비어 있습니다.");
            }
            return body;
        } catch (RestClientException exception) {
            log.warn("환율 조회 요청이 실패했습니다: {}", exception.getClass().getSimpleName());
            throw new ExchangeRateUnavailableException("환율 조회 요청이 실패했습니다.");
        }
    }

    private Map<CurrencyCode, ExchangeRate> toRates(CustomsExchangeRatePayload payload, Instant fetchedAt) {
        Map<CurrencyCode, ExchangeRate> rates = new EnumMap<>(CurrencyCode.class);
        for (CustomsExchangeRatePayload.Item item : payload.items()) {
            for (CurrencyCode currency : CurrencyCode.values()) {
                if (currency.name().equals(item.currSgn())) {
                    rates.put(currency, toRate(currency, item, fetchedAt));
                }
            }
        }
        return rates;
    }

    private ExchangeRate toRate(CurrencyCode currency, CustomsExchangeRatePayload.Item item, Instant fetchedAt) {
        try {
            BigDecimal krwPerUnit = new BigDecimal(item.fxrt());
            if (krwPerUnit.signum() <= 0) {
                throw new ExchangeRateUnavailableException(currency + " 환율이 0 이하입니다.");
            }
            return new ExchangeRate(currency, krwPerUnit, LocalDate.parse(item.aplyBgnDt(), DATE_FORMAT), fetchedAt);
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new ExchangeRateUnavailableException(currency + " 환율 응답 형식이 올바르지 않습니다.");
        }
    }

    private record Snapshot(LocalDate queriedOn, Map<CurrencyCode, ExchangeRate> rates) {
    }
}
