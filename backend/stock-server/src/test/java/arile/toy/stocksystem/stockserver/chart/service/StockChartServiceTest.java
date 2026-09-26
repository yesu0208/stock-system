package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.StubWebClients;
import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.token.ChartApiTokenManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("[Service] 일봉 차트 조회 테스트")
class StockChartServiceTest {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd");

    @DisplayName("100건씩 과거로 페이지를 넘겨 받고, 중복을 제거해 날짜 오름차순으로 돌려준다")
    @Test
    void givenPages_whenGettingDailyChart_thenPagesDedupesAndSorts() {
        List<ClientRequest> requests = new ArrayList<>();
        ChartApiTokenManager tokenManager = mock(ChartApiTokenManager.class);
        given(tokenManager.getAccessToken()).willReturn("token");
        var sut = withKeys(new StockChartService(StubWebClients.of(requests, request -> {
            LocalDate to = LocalDate.parse(StubWebClients.query(request, "FID_INPUT_DATE_2"), F);
            // 첫 요청은 100건(최신→과거), 두 번째 요청은 겹치는 1건 포함 3건
            int size = requests.size() == 1 ? 100 : 3;
            LocalDate start = requests.size() == 1 ? to : to.plusDays(1);
            return response(IntStream.range(0, size).mapToObj(start::minusDays).toList());
        }), tokenManager));

        List<CandleData> candles = sut.getDailyChart("005930", "20260101", "20260925");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).headers().getFirst("Authorization")).isEqualTo("Bearer token");
        assertThat(requests.get(0).headers().getFirst("tr_id")).isEqualTo("FHKST03010100");
        assertThat(StubWebClients.query(requests.get(1), "FID_INPUT_DATE_2")).isEqualTo("20260617");
        assertThat(candles).hasSize(102);
        assertThat(candles).extracting(CandleData::date).isSorted().doesNotHaveDuplicates();
        assertThat(candles.get(candles.size() - 1)).isEqualTo(new CandleData("20260925", 100, 120, 90, 110, 1000));
    }

    @DisplayName("from이 to보다 늦으면 바꿔서 조회하고, 빈 응답이면 빈 목록")
    @Test
    void givenReversedRangeAndEmpty_whenGettingDailyChart_thenEmpty() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockChartService(StubWebClients.of(requests, r -> "{\"rt_cd\":\"0\",\"output2\":[]}"),
                mock(ChartApiTokenManager.class)));

        assertThat(sut.getDailyChart("005930", "20260925", "20260101")).isEmpty();
        assertThat(StubWebClients.query(requests.get(0), "FID_INPUT_DATE_1")).isEqualTo("20260101");
        assertThat(StubWebClients.query(requests.get(0), "FID_INPUT_DATE_2")).isEqualTo("20260925");
    }

    @DisplayName("응답 본문이 없거나(null) output2가 없으면 조회를 멈추고 빈 목록을 돌려준다")
    @Test
    void givenNullBodyOrNoOutput_whenGettingDailyChart_thenEmpty() {
        var nullBody = withKeys(new StockChartService(StubWebClients.of(r -> null), mock(ChartApiTokenManager.class)));
        var noOutput = withKeys(new StockChartService(StubWebClients.of(r -> "{\"rt_cd\":\"1\",\"msg1\":\"error\"}"),
                mock(ChartApiTokenManager.class)));

        assertThat(nullBody.getDailyChart("005930", "20260101", "20260925")).isEmpty();
        assertThat(noOutput.getDailyChart("005930", "20260101", "20260925")).isEmpty();
    }

    @DisplayName("100건을 받아 다음 조회 기준일이 시작일보다 앞서면 반복 조건으로 종료한다")
    @Test
    void givenFullPageReachingFrom_whenGettingDailyChart_thenStopsByLoopCondition() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockChartService(StubWebClients.of(requests, request -> {
            LocalDate to = LocalDate.parse(StubWebClients.query(request, "FID_INPUT_DATE_2"), F);
            return response(IntStream.range(0, 100).mapToObj(to::minusDays).toList());
        }), mock(ChartApiTokenManager.class)));

        // 0925부터 100일 전(0618)까지 받으면 다음 기준일 0617이 시작일 0701보다 앞섬
        List<CandleData> candles = sut.getDailyChart("005930", "20260701", "20260925");

        assertThat(requests).hasSize(1);
        assertThat(candles).hasSize(100);
    }

    @DisplayName("응답의 가장 오래된 날짜가 요청 기준일보다 늦으면(진행 불가) 무한 반복 없이 종료한다")
    @Test
    void givenNoProgress_whenGettingDailyChart_thenStops() {
        List<ClientRequest> requests = new ArrayList<>();
        var sut = withKeys(new StockChartService(StubWebClients.of(requests, request ->
                // 요청 기준일(0925)보다 하루 늦은 날짜만 돌려줌 → 다음 기준일이 줄어들지 않음
                response(List.of(LocalDate.of(2026, 9, 26)))
        ), mock(ChartApiTokenManager.class)));

        List<CandleData> candles = sut.getDailyChart("005930", "20260101", "20260925");

        assertThat(requests).hasSize(1);
        assertThat(candles).extracting(CandleData::date).containsExactly("20260926");
    }

    private static <T> T withKeys(T service) {
        ReflectionTestUtils.setField(service, "appKey", "key");
        ReflectionTestUtils.setField(service, "appSecret", "secret");
        return service;
    }

    private String response(List<LocalDate> dates) {
        String items = dates.stream()
                .map(d -> "{\"stck_bsop_date\":\"%s\",\"stck_clpr\":\"110\",\"stck_oprc\":\"100\",\"stck_hgpr\":\"120\",\"stck_lwpr\":\"90\",\"acml_vol\":\"1000\"}"
                        .formatted(d.format(F)))
                .collect(Collectors.joining(","));
        return "{\"rt_cd\":\"0\",\"output2\":[" + items + "]}";
    }
}
