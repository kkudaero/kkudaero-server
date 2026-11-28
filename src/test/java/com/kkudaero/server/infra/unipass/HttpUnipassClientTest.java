package com.kkudaero.server.infra.unipass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClient;

class HttpUnipassClientTest {

    private static final String URL = "https://unipass.test/cargo";
    private static final String API_KEY = "test+key/value==";
    private static final String ENCODED_API_KEY = "test%2Bkey%2Fvalue%3D%3D";
    private static final String TRACKING_NUMBER = "5507410484";
    private static final String CARGO_MANAGEMENT_NUMBER = "26POZ06265I00110321";
    private static final Instant NOW = Instant.parse("2026-11-28T03:00:00Z");
    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int CACHE_MAX_SIZE = 2;
    private static final String EMPTY_BODY = """
            <cargCsclPrgsInfoQryRtnVo><ntceInfo></ntceInfo><tCnt>0</tCnt></cargCsclPrgsInfoQryRtnVo>
            """;
    private static final String INVALID_KEY_BODY = """
            <cargCsclPrgsInfoQryRtnVo>
              <ntceInfo>존재하지 않는 인증키입니다.</ntceInfo><tCnt>-1</tCnt>
            </cargCsclPrgsInfoQryRtnVo>
            """;
    private static final String SINGLE_BODY = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cargCsclPrgsInfoQryRtnVo>
              <ntceInfo></ntceInfo>
              <cargCsclPrgsInfoQryVo>
                <csclPrgsStts>통관목록심사완료</csclPrgsStts>
                <etprDt>20261127</etprDt>
                <hblNo>5507410484</hblNo>
                <prcsDttm>20261128101009</prcsDttm>
                <cargMtNo>26POZ06265I00110321</cargMtNo>
                <prgsStts>반출완료</prgsStts>
              </cargCsclPrgsInfoQryVo>
              <tCnt>2</tCnt>
              <cargCsclPrgsInfoDtlQryVo>
                <shedNm>특송물류센터</shedNm>
                <prcsDttm>20261128101009</prcsDttm>
                <cargTrcnRelaBsopTpcd>반출신고</cargTrcnRelaBsopTpcd>
                <rlbrCn>목록통관특송물품 반출</rlbrCn>
              </cargCsclPrgsInfoDtlQryVo>
              <cargCsclPrgsInfoDtlQryVo>
                <shedNm></shedNm>
                <prcsDttm></prcsDttm>
                <cargTrcnRelaBsopTpcd>입항보고 제출</cargTrcnRelaBsopTpcd>
                <rlbrCn></rlbrCn>
              </cargCsclPrgsInfoDtlQryVo>
            </cargCsclPrgsInfoQryRtnVo>
            """;
    private static final String MULTIPLE_BODY = """
            <cargCsclPrgsInfoQryRtnVo>
              <ntceInfo>[N00] 조회 결과가 다건입니다.</ntceInfo>
              <cargCsclPrgsInfoQryVo><cargMtNo>26OLD00000I00000001</cargMtNo><etprDt>20260105</etprDt>
              </cargCsclPrgsInfoQryVo>
              <cargCsclPrgsInfoQryVo><cargMtNo>26POZ06265I00110321</cargMtNo><etprDt>20261127</etprDt>
              </cargCsclPrgsInfoQryVo>
              <tCnt>2</tCnt>
            </cargCsclPrgsInfoQryRtnVo>
            """;

    private final Clock clock = mock(Clock.class);

    private MockRestServiceServer server;
    private HttpUnipassClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        when(clock.withZone(any())).thenAnswer(
                invocation -> Clock.fixed(clock.instant(), invocation.getArgument(0))
        );
        when(clock.instant()).thenReturn(NOW);
        client = client(builder.build(), API_KEY);
    }

    @Test
    void 운송장번호와_올해_연도로_조회해_도메인_타입으로_바꾼다() {
        server.expect(once(), requestTo(containsString(URL)))
                .andExpect(requestTo(containsString("crkyCn=" + ENCODED_API_KEY)))
                .andExpect(queryParam("hblNo", TRACKING_NUMBER))
                .andExpect(queryParam("blYy", "2026"))
                .andRespond(xml(SINGLE_BODY));

        CargoStatus status = client.getCargoStatus(TRACKING_NUMBER).orElseThrow();

        assertThat(status.cargoManagementNumber()).isEqualTo(CARGO_MANAGEMENT_NUMBER);
        assertThat(status.clearanceStatus()).isEqualTo("통관목록심사완료");
        assertThat(status.progressStatus()).isEqualTo("반출완료");
        assertThat(status.processedAt()).isEqualTo(Instant.parse("2026-11-28T01:10:09Z"));
        assertThat(status.fetchedAt()).isEqualTo(NOW);
        assertThat(status.events()).hasSize(2);
        assertThat(status.events().getFirst())
                .isEqualTo(new CargoEvent(
                        "반출신고",
                        Instant.parse("2026-11-28T01:10:09Z"),
                        "특송물류센터",
                        "목록통관특송물품 반출"
                ));
        assertThat(status.events().getLast().processedAt()).isNull();
        server.verify();
    }

    @Test
    void 올해_결과가_없으면_작년으로_다시_조회한다() {
        server.expect(once(), queryParam("blYy", "2026")).andRespond(xml(EMPTY_BODY));
        server.expect(once(), queryParam("blYy", "2025")).andRespond(xml(SINGLE_BODY));

        assertThat(client.getCargoStatus(TRACKING_NUMBER)).isPresent();
        server.verify();
    }

    @Test
    void 올해와_작년_모두_결과가_없으면_빈_값을_돌려준다() {
        server.expect(once(), queryParam("blYy", "2026")).andRespond(xml(EMPTY_BODY));
        server.expect(once(), queryParam("blYy", "2025")).andRespond(xml(EMPTY_BODY));

        assertThat(client.getCargoStatus(TRACKING_NUMBER)).isEmpty();
        server.verify();
    }

    @Test
    void 화물이_여러_건이면_가장_최근에_입항한_화물관리번호로_다시_조회한다() {
        server.expect(once(), queryParam("hblNo", TRACKING_NUMBER)).andRespond(xml(MULTIPLE_BODY));
        server.expect(once(), queryParam("cargMtNo", CARGO_MANAGEMENT_NUMBER)).andRespond(xml(SINGLE_BODY));

        Optional<CargoStatus> status = client.getCargoStatus(TRACKING_NUMBER);

        assertThat(status).map(CargoStatus::progressStatus).contains("반출완료");
        server.verify();
    }

    @Test
    void 캐시_유효_시간_안에는_한_번만_조회하고_지나면_다시_조회한다() {
        server.expect(once(), requestTo(containsString(URL))).andRespond(xml(SINGLE_BODY));
        server.expect(once(), requestTo(containsString(URL))).andRespond(xml(SINGLE_BODY));

        client.getCargoStatus(TRACKING_NUMBER);
        when(clock.instant()).thenReturn(NOW.plus(CACHE_TTL).minusSeconds(1));
        CargoStatus cached = client.getCargoStatus(TRACKING_NUMBER).orElseThrow();
        when(clock.instant()).thenReturn(NOW.plus(CACHE_TTL));
        CargoStatus refreshed = client.getCargoStatus(TRACKING_NUMBER).orElseThrow();

        assertThat(cached.fetchedAt()).isEqualTo(NOW);
        assertThat(refreshed.fetchedAt()).isEqualTo(NOW.plus(CACHE_TTL));
        server.verify();
    }

    @Test
    void 캐시가_가득_차면_가장_오래된_운송장번호부터_버린다() {
        for (int count = 0; count < CACHE_MAX_SIZE + 2; count++) {
            server.expect(once(), requestTo(containsString(URL))).andRespond(xml(SINGLE_BODY));
        }

        client.getCargoStatus("first");
        client.getCargoStatus("second");
        client.getCargoStatus("third");
        client.getCargoStatus("third");
        client.getCargoStatus("first");

        server.verify();
    }

    @Test
    void 인증키가_없으면_요청하지_않고_실패한다() {
        HttpUnipassClient clientWithoutKey = client(RestClient.builder().build(), " ");

        assertThatThrownBy(() -> clientWithoutKey.getCargoStatus(TRACKING_NUMBER))
                .isInstanceOf(UnipassUnavailableException.class);
    }

    @Test
    void 관세청이_거절하면_인증키를_노출하지_않고_실패하고_캐시하지_않는다() {
        server.expect(once(), requestTo(containsString(URL))).andRespond(xml(INVALID_KEY_BODY));
        server.expect(once(), requestTo(containsString(URL))).andRespond(xml(SINGLE_BODY));

        assertThatThrownBy(() -> client.getCargoStatus(TRACKING_NUMBER))
                .isInstanceOf(UnipassUnavailableException.class)
                .hasMessageNotContaining(API_KEY)
                .hasMessageNotContaining(ENCODED_API_KEY);
        assertThat(client.getCargoStatus(TRACKING_NUMBER)).isPresent();
        server.verify();
    }

    @Test
    void 서버_오류나_읽을_수_없는_응답이면_실패한다() {
        server.expect(once(), requestTo(containsString(URL))).andRespond(withServerError());
        server.expect(once(), requestTo(containsString(URL)))
                .andRespond(withSuccess("not xml", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.getCargoStatus(TRACKING_NUMBER))
                .isInstanceOf(UnipassUnavailableException.class);
        assertThatThrownBy(() -> client.getCargoStatus(TRACKING_NUMBER))
                .isInstanceOf(UnipassUnavailableException.class);
    }

    private DefaultResponseCreator xml(String body) {
        return withSuccess(body.strip().getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML);
    }

    private HttpUnipassClient client(RestClient restClient, String apiKey) {
        return new HttpUnipassClient(
                restClient,
                new UnipassProperties(URL, apiKey, TIMEOUT, TIMEOUT, CACHE_TTL, CACHE_MAX_SIZE),
                clock
        );
    }
}
