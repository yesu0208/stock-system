package arile.toy.stocksystem.stockserver.chart.scheduler;

import arile.toy.stocksystem.stockserver.chart.service.ChartCacheService;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.*;

@DisplayName("[Scheduler] 차트 캐시 갱신 스케줄 테스트")
@ExtendWith(MockitoExtension.class)
class ChartCacheSchedulerTest {

    @InjectMocks private ChartCacheScheduler sut;
    @Mock private ExternalStockProperties stockProperties;
    @Mock private ChartCacheService chartCacheService;

    @BeforeEach
    void setUp() {
        given(stockProperties.getOpen()).willReturn(List.of("005930", "000660"));
    }

    @DisplayName("초기 적재는 담당 종목 전체의 일봉·분봉을 갱신한다")
    @Test
    void whenInitialFill_thenRefreshesBoth() {
        sut.initialFill();

        then(chartCacheService).should().refreshDailyChart("005930");
        then(chartCacheService).should().refreshDailyChart("000660");
        then(chartCacheService).should().refreshMinuteChart("005930");
        then(chartCacheService).should().refreshMinuteChart("000660");
    }

    @DisplayName("분봉 스케줄은 분봉만, 일봉 스케줄은 일봉만 갱신한다")
    @Test
    void whenScheduled_thenRefreshesByKind() {
        sut.refreshMinuteChartsMorningToAfternoon();
        sut.refreshMinuteChartsClosingWindow();
        sut.refreshDailyCharts();

        then(chartCacheService).should(times(2)).refreshMinuteChart("005930");
        then(chartCacheService).should(times(1)).refreshDailyChart("005930");
    }
}
