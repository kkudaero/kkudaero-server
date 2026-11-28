package com.kkudaero.server.infra.exchange;

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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExchangeRateClientTest {

    private static final String URL = "https://exchange.test/rates";
    private static final String SERVICE_KEY = "test+key/value==";
    private static final String ENCODED_SERVICE_KEY = "test%2Bkey%2Fvalue%3D%3D";
    private static final Instant NOW = Instant.parse("2026-11-27T16:00:00Z");
    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    private static final String SUCCESS_BODY = """
            <response>
              <header><resultCode>00</resultCode><resultMsg>정상서비스.</resultMsg></header>
              <body><items>
                <item><aplyBgnDt>20261122</aplyBgnDt><currSgn>JPY</currSgn><fxrt>8.3599</fxrt></item>
                <item><currSgn>USD</currSgn><fxrt>1333.88</fxrt><aplyBgnDt>20261122</aplyBgnDt></item>
                <item><currSgn>EUR</currSgn><fxrt>1466.2</fxrt><aplyBgnDt>20261122</aplyBgnDt></item>
              </items></body>
            </response>
            """;

    private final Clock clock = mock(Clock.class);

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        when(clock.withZone(any())).thenAnswer(
                invocation -> Clock.fixed(clock.instant(), invocation.getArgument(0))
        );
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void 수입_과세환율을_한국_날짜로_조회해_도메인_타입으로_바꾼다() {
        server.expect(once(), requestTo(containsString(URL)))
                .andExpect(requestTo(containsString("serviceKey=" + ENCODED_SERVICE_KEY)))
                .andExpect(queryParam("aplyBgnDt", "20261128"))
                .andExpect(queryParam("weekFxrtTpcd", "2"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_XML));

        ExchangeRate rate = client(SERVICE_KEY).getImportRate(CurrencyCode.JPY);

        assertThat(rate.krwPerUnit()).isEqualByComparingTo("8.3599");
        assertThat(rate.appliedFrom()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(rate.fetchedAt()).isEqualTo(NOW);
        server.verify();
    }

    @Test
    void 인증키_앞뒤의_공백과_줄바꿈은_떼고_보낸다() {
        server.expect(once(), requestTo(containsString("serviceKey=" + ENCODED_SERVICE_KEY + "&")))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_XML));

        client(" " + SERVICE_KEY + "\n").getImportRate(CurrencyCode.JPY);

        server.verify();
    }

    @Test
    void 같은_날에는_한_번만_조회한다() {
        server.expect(once(), requestTo(containsString(URL)))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_XML));
        HttpExchangeRateClient client = client(SERVICE_KEY);

        client.getImportRate(CurrencyCode.JPY);
        ExchangeRate usd = client.getImportRate(CurrencyCode.USD);

        assertThat(usd.krwPerUnit()).isEqualByComparingTo("1333.88");
        server.verify();
    }

    @Test
    void 날짜가_바뀌면_다시_조회한다() {
        server.expect(once(), queryParam("aplyBgnDt", "20261128"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_XML));
        server.expect(once(), queryParam("aplyBgnDt", "20261129"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_XML));
        HttpExchangeRateClient client = client(SERVICE_KEY);

        client.getImportRate(CurrencyCode.JPY);
        when(clock.instant()).thenReturn(NOW.plus(Duration.ofDays(1)));
        client.getImportRate(CurrencyCode.JPY);

        server.verify();
    }

    @Test
    void 인증키가_없으면_요청하지_않고_실패한다() {
        assertThatThrownBy(() -> client("").getImportRate(CurrencyCode.JPY))
                .isInstanceOf(ExchangeRateUnavailableException.class);
        server.verify();
    }

    @Test
    void 결과_코드가_정상이_아니면_실패한다() {
        server.expect(once(), requestTo(containsString(URL)))
                .andRespond(withSuccess(SUCCESS_BODY.replace(">00<", ">99<"), MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client(SERVICE_KEY).getImportRate(CurrencyCode.JPY))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void 응답에_통화가_없으면_실패한다() {
        server.expect(once(), requestTo(containsString(URL)))
                .andRespond(withSuccess(SUCCESS_BODY.replace("JPY", "CNY"), MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client(SERVICE_KEY).getImportRate(CurrencyCode.JPY))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void 서버_오류나_읽을_수_없는_응답이면_인증키를_노출하지_않고_실패한다() {
        server.expect(once(), requestTo(containsString(URL))).andRespond(withServerError());
        server.expect(once(), requestTo(containsString(URL)))
                .andRespond(withSuccess("not xml", MediaType.TEXT_PLAIN));
        HttpExchangeRateClient client = client(SERVICE_KEY);

        assertThatThrownBy(() -> client.getImportRate(CurrencyCode.JPY))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageNotContaining(SERVICE_KEY)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
        assertThatThrownBy(() -> client.getImportRate(CurrencyCode.JPY))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private HttpExchangeRateClient client(String serviceKey) {
        return new HttpExchangeRateClient(
                restClient,
                new ExchangeProperties(URL, serviceKey, TIMEOUT, TIMEOUT),
                clock
        );
    }
}
