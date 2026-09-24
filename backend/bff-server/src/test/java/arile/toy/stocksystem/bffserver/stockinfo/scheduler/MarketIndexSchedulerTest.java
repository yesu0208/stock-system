package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.MarketMainResponse;
import arile.toy.stocksystem.bffserver.stockinfo.event.MarketMainRedisPublisher;
import arile.toy.stocksystem.bffserver.stockinfo.repository.MarketMainSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MarketIndexSchedulerTest {

    private static final String LOCK_KEY = "market:main:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(8);

    private NaverStockCrawlerClient crawlerClient;
    private MarketMainSnapshotRepository snapshotRepository;
    private MarketMainRedisPublisher publisher;
    private ValueOperations<String, String> valueOps;
    private MarketIndexScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        crawlerClient = mock(NaverStockCrawlerClient.class);
        snapshotRepository = mock(MarketMainSnapshotRepository.class);
        publisher = mock(MarketMainRedisPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        scheduler = new MarketIndexScheduler(crawlerClient, redisTemplate, snapshotRepository, publisher);
    }

    @Test
    @DisplayName("락을 잡으면 지수를 크롤링해 스냅샷을 먼저 저장하고, 이어서 발행한다")
    void lockAcquired() {
        MarketMainResponse response = mock(MarketMainResponse.class);
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getMarketIndices()).willReturn(response);

        scheduler.broadcastMarketIndices();

        InOrder inOrder = inOrder(snapshotRepository, publisher);
        inOrder.verify(snapshotRepository).save(response);
        inOrder.verify(publisher).publish(response);
    }

    @Test
    @DisplayName("락을 잡지 못하면(다른 인스턴스 담당) 크롤링하지 않는다")
    void lockNotAcquired() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(false);

        scheduler.broadcastMarketIndices();

        verifyNoInteractions(crawlerClient, snapshotRepository, publisher);
    }

    @Test
    @DisplayName("크롤링이 실패해도 예외를 던지지 않고 저장·발행하지 않는다")
    void crawlFails() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getMarketIndices()).willThrow(new IllegalStateException("naver down"));

        assertThatCode(() -> scheduler.broadcastMarketIndices()).doesNotThrowAnyException();

        verifyNoInteractions(snapshotRepository, publisher);
    }
}
