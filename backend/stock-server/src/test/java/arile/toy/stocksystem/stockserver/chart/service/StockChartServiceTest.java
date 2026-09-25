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
