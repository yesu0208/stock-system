package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.StubWebClients;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.token.ChartApiTokenManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("[Service] 분봉 차트 조회 테스트")
class StockMinuteChartServiceTest {

    @DisplayName("가장 오래된 봉의 1초 전으로 이어서 조회하고, 장 시작 전으로 내려가면 전날 15:30:00으로 넘어간다")
    @Test
    void givenBatches_whenGettingMinuteChart_thenContinuesBackward() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request -> {
            String date = StubWebClients.query(request, "FID_INPUT_DATE_1");
            String hour = StubWebClients.query(request, "FID_INPUT_HOUR_1");
            if (date.equals("20260925") && hour.equals("100000")) {
                return response(date, "100000", "090000");
            }
            if (date.equals("20260924") && hour.equals("153000")) {
                return response(date, "153000", "152900");
            }
            return "{\"rt_cd\":\"0\",\"output2\":[]}";
        }), mock(ChartApiTokenManager.class)));

        List<MinuteCandle> candles = sut.getMinuteChart("005930", "20260925", "100000", 4);

        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_1")).isEqualTo("20260924");
        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_HOUR_1")).isEqualTo("153000");
        assertThat(candles).extracting(c -> c.date() + c.time())
                .containsExactly("20260924152900", "20260924153000", "20260925090000", "20260925100000");
    }

    @DisplayName("응답이 계속 비어 있으면(토큰 없음·API 오류 등) 연속 10일에서 멈춘다 (무한 호출 방지)")
    @Test
    void givenAlwaysEmpty_whenGettingMinuteChart_thenStopsAfterLimit() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, r -> "{\"rt_cd\":\"1\",\"msg1\":\"error\"}"),
                mock(ChartApiTokenManager.class)));

        assertThat(sut.getMinuteChart("005930", "20260925", "100000", 500)).isEmpty();
        assertThat(requests).hasSize(10);
    }

    private static <T> T withKeys(T service) {
        ReflectionTestUtils.setField(service, "appKey", "key");
        ReflectionTestUtils.setField(service, "appSecret", "secret");
        return service;
    }

    @DisplayName("날짜가 없거나 빈 항목은 걸러내고, 전부 걸러지면 빈 응답처럼 전날로 넘어간다")
    @Test
    void givenItemsWithoutDate_whenGettingMinuteChart_thenFiltersAndJumps() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request -> {
            String date = StubWebClients.query(request, "FID_INPUT_DATE_1");
            if (date.equals("20260925")) {
                // 날짜 없음(null)·공백 항목만 있는 응답
                return "{\"rt_cd\":\"0\",\"output2\":["
                        + "{\"stck_cntg_hour\":\"100000\",\"stck_prpr\":\"1\",\"stck_oprc\":\"1\",\"stck_hgpr\":\"1\",\"stck_lwpr\":\"1\",\"cntg_vol\":\"1\"},"
                        + "{\"stck_bsop_date\":\" \",\"stck_cntg_hour\":\"095900\",\"stck_prpr\":\"1\",\"stck_oprc\":\"1\",\"stck_hgpr\":\"1\",\"stck_lwpr\":\"1\",\"cntg_vol\":\"1\"}]}";
            }
            if (date.equals("20260924")) {
                return response(date, "153000");
            }
            return "{\"rt_cd\":\"0\",\"output2\":[]}";
        }), mock(ChartApiTokenManager.class)));

        List<MinuteCandle> candles = sut.getMinuteChart("005930", "20260925", "100000", 1);

        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_1")).isEqualTo("20260924");
        assertThat(candles).extracting(c -> c.date() + c.time()).containsExactly("20260924153000");
    }

    @DisplayName("체결 시각이 000000이면 음수로 내려가지 않고 전날 장 마감 시각으로 넘어간다")
    @Test
    void givenMidnightCandle_whenGettingMinuteChart_thenJumpsToPreviousDay() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request -> {
            String date = StubWebClients.query(request, "FID_INPUT_DATE_1");
            return date.equals("20260925") ? response(date, "000000") : response(date, "153000");
        }), mock(ChartApiTokenManager.class)));

        sut.getMinuteChart("005930", "20260925", "100000", 2);

        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_1")).isEqualTo("20260924");
        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_HOUR_1")).isEqualTo("153000");
    }

    @DisplayName("응답이 조금씩만 오면 최대 요청 횟수(50회)에서 멈춘다")
    @Test
    void givenTinyBatches_whenGettingMinuteChart_thenStopsAtMaxRequests() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request ->
                response(StubWebClients.query(request, "FID_INPUT_DATE_1"),
                        StubWebClients.query(request, "FID_INPUT_HOUR_1"))
        ), mock(ChartApiTokenManager.class)));

        List<MinuteCandle> candles = sut.getMinuteChart("005930", "20260925", "100000", 500);

        assertThat(requests).hasSize(50);
        assertThat(candles).hasSize(50);
    }

    @DisplayName("응답 본문이 비어 있으면(null) 빈 응답처럼 전날로 넘어간다")
    @Test
    void givenEmptyBody_whenGettingMinuteChart_thenJumpsToPreviousDay() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request ->
                StubWebClients.query(request, "FID_INPUT_DATE_1").equals("20260925")
                        ? "null"   // 본문 없음 -> bodyToMono가 비어 block()이 null을 돌려줌
                        : response(StubWebClients.query(request, "FID_INPUT_DATE_1"), "153000")
        ), mock(ChartApiTokenManager.class)));

        List<MinuteCandle> candles = sut.getMinuteChart("005930", "20260925", "100000", 1);

        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_1")).isEqualTo("20260924");
        assertThat(candles).extracting(c -> c.date() + c.time()).containsExactly("20260924153000");
    }

    @DisplayName("output2가 빈 배열이면 빈 응답처럼 전날로 넘어간다")
    @Test
    void givenEmptyOutput_whenGettingMinuteChart_thenJumpsToPreviousDay() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockMinuteChartService(StubWebClients.of(requests, request -> {
            String date = StubWebClients.query(request, "FID_INPUT_DATE_1");
            return date.equals("20260925")
                    ? "{\"rt_cd\":\"0\",\"output2\":[]}"
                    : response(date, "153000");
        }), mock(ChartApiTokenManager.class)));

        List<MinuteCandle> candles = sut.getMinuteChart("005930", "20260925", "100000", 1);

        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_1")).isEqualTo("20260924");
        assertThat(candles).extracting(c -> c.date() + c.time()).containsExactly("20260924153000");
    }

    private String response(String date, String... hours) {
        List<String> items = new ArrayList<>();
        for (String h : hours) {
            items.add("{\"stck_bsop_date\":\"%s\",\"stck_cntg_hour\":\"%s\",\"stck_prpr\":\"110\",\"stck_oprc\":\"100\",\"stck_hgpr\":\"120\",\"stck_lwpr\":\"90\",\"cntg_vol\":\"10\"}"
                    .formatted(date, h));
        }
        return "{\"rt_cd\":\"0\",\"output2\":[" + String.join(",", items) + "]}";
    }
}
