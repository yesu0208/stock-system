package arile.toy.stocksystem.bffserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class StockDetailCrawlExecutorConfigTest {

    @Test
    @DisplayName("크롤링용으로 스레드 4개 고정 크기 풀을 생성한다")
    void stockDetailCrawlExecutor() {
        ExecutorService executor = new StockDetailCrawlExecutorConfig().stockDetailCrawlExecutor();
        try {
            assertThat(executor).isInstanceOf(ThreadPoolExecutor.class);
            ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
            assertThat(pool.getCorePoolSize()).isEqualTo(4);
            assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }
}
