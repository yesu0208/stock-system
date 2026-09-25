package arile.toy.stocksystem.stockserver.chart.service;

import arile.toy.stocksystem.stockserver.chart.dto.CandleData;
import arile.toy.stocksystem.stockserver.chart.dto.MinuteCandle;
import arile.toy.stocksystem.stockserver.chart.repository.ChartSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Service] 차트 캐시 갱신 테스트")
@ExtendWith(MockitoExtension.class)
class ChartCacheServiceTest {

    @InjectMocks private ChartCacheService sut;
    @Mock private StockChartService stockChartService;
    @Mock private StockMinuteChartService stockMinuteChartService;
    @Mock private ChartSnapshotRepository chartSnapshotRepository;

    @DisplayName("일봉은 한국 날짜 기준 최근 12개월, 분봉은 현재 시각부터 500개를 조회해 캐시에 저장한다")
    @Test
    void whenRefreshing_thenFetchesAndSaves() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
        List<CandleData> daily = List.of(new CandleData("20260925", 1, 1, 1, 1, 1));
        List<MinuteCandle> minute = List.of(new MinuteCandle("20260925", "093000", 1, 1, 1, 1, 1));
        given(stockChartService.getDailyChart("005930", today.minusMonths(12).format(f), today.format(f))).willReturn(daily);
        given(stockMinuteChartService.getMinuteChart(eq("005930"), eq(today.format(f)), anyString(), eq(500))).willReturn(minute);

        sut.refreshDailyChart("005930");
        sut.refreshMinuteChart("005930");

        then(chartSnapshotRepository).should().saveDaily("005930", daily);
        then(chartSnapshotRepository).should().saveMinute("005930", minute);
    }

    @DisplayName("조회가 실패해도 예외를 던지지 않고 캐시를 건드리지 않는다")
    @Test
    void givenFetchFails_whenRefreshing_thenSwallows() {
        given(stockChartService.getDailyChart(anyString(), anyString(), anyString())).willThrow(new IllegalStateException("down"));
        given(stockMinuteChartService.getMinuteChart(anyString(), anyString(), anyString(), anyInt()))
                .willThrow(new IllegalStateException("down"));

        assertThatNoException().isThrownBy(() -> {
            sut.refreshDailyChart("005930");
            sut.refreshMinuteChart("005930");
        });
        then(chartSnapshotRepository).shouldHaveNoInteractions();
    }
}
