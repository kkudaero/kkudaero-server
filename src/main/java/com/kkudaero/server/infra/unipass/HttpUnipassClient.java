package com.kkudaero.server.infra.unipass;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class HttpUnipassClient implements UnipassClient {

    private static final ZoneId CUSTOMS_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter PROCESSED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int SEARCH_YEARS = 2;
    private static final String API_KEY_PARAM = "crkyCn";
    private static final String HOUSE_BL_NUMBER_PARAM = "hblNo";
    private static final String BL_YEAR_PARAM = "blYy";
    private static final String CARGO_MANAGEMENT_NUMBER_PARAM = "cargMtNo";

    private final RestClient unipassRestClient;
    private final UnipassProperties properties;
    private final Clock clock;
    private final Map<String, CachedStatus> cache;

    public HttpUnipassClient(RestClient unipassRestClient, UnipassProperties properties, Clock clock) {
        this.unipassRestClient = unipassRestClient;
        this.properties = properties;
        this.clock = clock;
        this.cache = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedStatus> eldest) {
                return size() > properties.cacheMaxSize();
            }
        };
    }

    @Override
    public Optional<CargoStatus> getCargoStatus(String trackingNumber) {
        Instant now = clock.instant();
        Optional<CachedStatus> cached = findCached(trackingNumber, now);
        if (cached.isPresent()) {
            return cached.get().status();
        }

        Optional<CargoStatus> status = fetchCargoStatus(trackingNumber, now);
        putCache(trackingNumber, new CachedStatus(status, now.plus(properties.cacheTtl())));
        return status;
    }

    private synchronized Optional<CachedStatus> findCached(String trackingNumber, Instant now) {
        CachedStatus cached = cache.get(trackingNumber);
        if (cached == null) {
            return Optional.empty();
        }
        if (!now.isBefore(cached.expiresAt())) {
            cache.remove(trackingNumber);
            return Optional.empty();
        }
        return Optional.of(cached);
    }

    private synchronized void putCache(String trackingNumber, CachedStatus status) {
        cache.remove(trackingNumber);
        cache.put(trackingNumber, status);
    }

    private Optional<CargoStatus> fetchCargoStatus(String trackingNumber, Instant now) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new UnipassUnavailableException("통관 조회 인증키가 설정되지 않았습니다.");
        }

        int thisYear = Year.now(clock.withZone(CUSTOMS_ZONE)).getValue();
        for (int yearsAgo = 0; yearsAgo < SEARCH_YEARS; yearsAgo++) {
            UnipassCargoPayload payload = request(Map.of(
                    HOUSE_BL_NUMBER_PARAM, trackingNumber,
                    BL_YEAR_PARAM, String.valueOf(thisYear - yearsAgo)
            ));
            if (!payload.summaries().isEmpty()) {
                return Optional.of(toCargoStatus(resolveSingleCargo(payload), now));
            }
        }
        return Optional.empty();
    }

    private UnipassCargoPayload resolveSingleCargo(UnipassCargoPayload payload) {
        if (!payload.hasMultipleCargos()) {
            return payload;
        }

        String latestCargoManagementNumber = payload.summaries().stream()
                .max(Comparator.comparing(UnipassCargoPayload.Summary::etprDt))
                .map(UnipassCargoPayload.Summary::cargMtNo)
                .orElseThrow();
        UnipassCargoPayload single = request(Map.of(CARGO_MANAGEMENT_NUMBER_PARAM, latestCargoManagementNumber));
        if (single.summaries().isEmpty() || single.hasMultipleCargos()) {
            throw new UnipassUnavailableException("화물관리번호로 통관 진행 정보를 찾지 못했습니다.");
        }
        return single;
    }

    private UnipassCargoPayload request(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.cargoProgressUrl())
                .queryParam(API_KEY_PARAM, "{apiKey}");
        params.forEach(builder::queryParam);
        URI uri = builder.encode().buildAndExpand(properties.apiKey()).toUri();

        byte[] body;
        try {
            body = unipassRestClient.get().uri(uri).retrieve().body(byte[].class);
        } catch (RestClientException exception) {
            log.warn("통관 진행 조회 요청이 실패했습니다: {}", exception.getClass().getSimpleName());
            throw new UnipassUnavailableException("통관 진행 조회 요청이 실패했습니다.");
        }
        if (body == null || body.length == 0) {
            throw new UnipassUnavailableException("통관 진행 응답이 비어 있습니다.");
        }

        UnipassCargoPayload payload = UnipassCargoPayload.parse(body);
        if (payload.isError()) {
            log.warn("통관 진행 조회가 거절됐습니다: {}", payload.notice());
            throw new UnipassUnavailableException("통관 진행 조회가 거절됐습니다.");
        }
        return payload;
    }

    private CargoStatus toCargoStatus(UnipassCargoPayload payload, Instant fetchedAt) {
        UnipassCargoPayload.Summary summary = payload.summaries().getFirst();
        return new CargoStatus(
                summary.cargMtNo(),
                summary.csclPrgsStts(),
                summary.prgsStts(),
                toInstant(summary.prcsDttm()),
                payload.details().stream().map(this::toCargoEvent).toList(),
                fetchedAt
        );
    }

    private CargoEvent toCargoEvent(UnipassCargoPayload.Detail detail) {
        return new CargoEvent(
                detail.cargTrcnRelaBsopTpcd(),
                toInstant(detail.prcsDttm()),
                detail.shedNm(),
                detail.rlbrCn()
        );
    }

    private Instant toInstant(String processedAt) {
        try {
            return LocalDateTime.parse(processedAt, PROCESSED_AT_FORMAT).atZone(CUSTOMS_ZONE).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private record CachedStatus(Optional<CargoStatus> status, Instant expiresAt) {
    }
}
