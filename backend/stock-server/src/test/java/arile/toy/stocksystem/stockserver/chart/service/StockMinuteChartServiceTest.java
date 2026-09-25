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

    private String response(String date, String... hours) {
        List<String> items = new ArrayList<>();
        for (String h : hours) {
            items.add("{\"stck_bsop_date\":\"%s\",\"stck_cntg_hour\":\"%s\",\"stck_prpr\":\"110\",\"stck_oprc\":\"100\",\"stck_hgpr\":\"120\",\"stck_lwpr\":\"90\",\"cntg_vol\":\"10\"}"
                    .formatted(date, h));
        }
        return "{\"rt_cd\":\"0\",\"output2\":[" + String.join(",", items) + "]}";
    }
}
