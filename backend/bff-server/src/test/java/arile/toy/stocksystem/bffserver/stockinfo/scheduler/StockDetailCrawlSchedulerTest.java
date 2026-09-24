package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.registry.StockDetailWatchRegistry;
import arile.toy.stocksystem.bffserver.stockinfo.service.StockDetailCrawlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockDetailCrawlSchedulerTest {

    @Mock private StockDetailWatchRegistry watchRegistry;
    @Mock private StockDetailCrawlService crawlService;
    @Mock private ExecutorService stockDetailCrawlExecutor;

    @InjectMocks
    private StockDetailCrawlScheduler scheduler;

    @Test
    @DisplayName("감시 중인 종목마다 크롤링을 전용 스레드 풀에 맡긴다 (스케줄러 스레드에서 직접 호출하지 않음)")
    void submitsEachActiveStock() {
        given(watchRegistry.getActiveCodes()).willReturn(new LinkedHashSet<>(List.of("005930", "000660")));

        scheduler.crawlActiveStocks();

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(stockDetailCrawlExecutor, times(2)).submit(tasks.capture());
        verifyNoInteractions(crawlService);

        tasks.getAllValues().forEach(Runnable::run);
        verify(crawlService).crawlAndPublish("005930");
        verify(crawlService).crawlAndPublish("000660");
    }

    @Test
    @DisplayName("감시 중인 종목이 없거나 조회 결과가 null이면 아무것도 하지 않는다")
    void noActiveStocks() {
        given(watchRegistry.getActiveCodes()).willReturn(Set.of()).willReturn(null);

        scheduler.crawlActiveStocks();
        scheduler.crawlActiveStocks();

        verifyNoInteractions(stockDetailCrawlExecutor, crawlService);
    }

    @Test
    @DisplayName("스레드 풀이 작업을 거절해도(서버 종료 중) 예외 없이 나머지 종목 등록을 계속한다")
    void rejected_continues() {
        given(watchRegistry.getActiveCodes()).willReturn(new LinkedHashSet<>(List.of("005930", "000660")));
        given(stockDetailCrawlExecutor.submit(any(Runnable.class)))
                .willThrow(new RejectedExecutionException("shutdown"))
                .willReturn(null);

        assertThatCode(() -> scheduler.crawlActiveStocks()).doesNotThrowAnyException();

        verify(stockDetailCrawlExecutor, times(2)).submit(any(Runnable.class));
    }
}
