package arile.toy.stocksystem.bffserver.stockinfo.service;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.StockDetailTickMessage;
import arile.toy.stocksystem.bffserver.stockinfo.event.StockDetailRedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StockDetailCrawlServiceTest {

    private static final String LOCK_KEY = "stock:detail:lock:005930";
    private static final Duration LOCK_TTL = Duration.ofSeconds(4);

    private NaverStockCrawlerClient crawlerClient;
    private StockDetailRedisPublisher publisher;
    private ValueOperations<String, String> valueOps;
    private StockDetailCrawlService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        crawlerClient = mock(NaverStockCrawlerClient.class);
        publisher = mock(StockDetailRedisPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        service = new StockDetailCrawlService(crawlerClient, publisher, redisTemplate);
    }

    @Test
    @DisplayName("4초 락을 잡으면 종목 상세를 크롤링해 발행한다")
    void lockAcquired_crawlsAndPublishes() {
        StockDetailTickMessage message = mock(StockDetailTickMessage.class);
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getStockDetailSummary("005930")).willReturn(message);

        service.crawlAndPublish("005930");

        verify(publisher).publish(message);
    }

    @Test
    @DisplayName("락을 잡지 못하면(다른 인스턴스가 방금 처리) 크롤링하지 않는다")
    void lockNotAcquired_skips() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(false);

        service.crawlAndPublish("005930");

        verifyNoInteractions(crawlerClient, publisher);
    }

    @Test
    @DisplayName("락 결과가 null이어도 크롤링하지 않는다")
    void lockNull_skips() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(null);

        service.crawlAndPublish("005930");

        verifyNoInteractions(crawlerClient, publisher);
    }

    @Test
    @DisplayName("락 획득 중 Redis 오류가 나도 예외를 던지지 않는다 (스레드 풀에서 로그 없이 사라지지 않도록 기록)")
    void lockFails_swallowed() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL))
                .willThrow(new RedisConnectionFailureException("redis down"));

        assertThatCode(() -> service.crawlAndPublish("005930")).doesNotThrowAnyException();

        verifyNoInteractions(crawlerClient, publisher);
    }

    @Test
    @DisplayName("크롤링이 실패해도 예외를 던지지 않고 발행하지 않는다 (다음 주기에 재시도)")
    void crawlFails_swallowed() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getStockDetailSummary("005930")).willThrow(new IllegalStateException("naver down"));

        assertThatCode(() -> service.crawlAndPublish("005930")).doesNotThrowAnyException();

        verifyNoInteractions(publisher);
    }
}
