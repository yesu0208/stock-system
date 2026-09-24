package arile.toy.stocksystem.bffserver.stockinfo.scheduler;

import arile.toy.stocksystem.bffserver.stockinfo.client.NaverStockCrawlerClient;
import arile.toy.stocksystem.bffserver.stockinfo.dto.ExchangeRateDto;
import arile.toy.stocksystem.bffserver.stockinfo.dto.GlobalMarketResponse;
import arile.toy.stocksystem.bffserver.stockinfo.event.GlobalMarketRedisPublisher;
import arile.toy.stocksystem.bffserver.stockinfo.repository.GlobalMarketSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalMarketSchedulerTest {

    private static final String LOCK_KEY = "market:global:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(8);

    private NaverStockCrawlerClient crawlerClient;
    private GlobalMarketSnapshotRepository snapshotRepository;
    private GlobalMarketRedisPublisher publisher;
    private ValueOperations<String, String> valueOps;
    private GlobalMarketScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        crawlerClient = mock(NaverStockCrawlerClient.class);
        snapshotRepository = mock(GlobalMarketSnapshotRepository.class);
        publisher = mock(GlobalMarketRedisPublisher.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        doReturn(valueOps).when(redisTemplate).opsForValue();
        scheduler = new GlobalMarketScheduler(crawlerClient, redisTemplate, snapshotRepository, publisher);
    }

    @Test
    @DisplayName("락을 잡으면 환율을 크롤링해 스냅샷을 먼저 저장하고, 이어서 발행한다")
    void lockAcquired() {
        List<ExchangeRateDto> rates = List.of(mock(ExchangeRateDto.class));
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getExchangeRates()).willReturn(rates);

        scheduler.broadcastGlobalMarket();

        GlobalMarketResponse expected = new GlobalMarketResponse(rates);
        InOrder inOrder = inOrder(snapshotRepository, publisher);
        inOrder.verify(snapshotRepository).save(expected);
        inOrder.verify(publisher).publish(expected);
    }

    @Test
    @DisplayName("락을 잡지 못하면(다른 인스턴스 담당) 크롤링하지 않는다")
    void lockNotAcquired() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(false);

        scheduler.broadcastGlobalMarket();

        verifyNoInteractions(crawlerClient, snapshotRepository, publisher);
    }

    @Test
    @DisplayName("크롤링이 실패해도 예외를 던지지 않고 저장·발행하지 않는다")
    void crawlFails() {
        given(valueOps.setIfAbsent(LOCK_KEY, "1", LOCK_TTL)).willReturn(true);
        given(crawlerClient.getExchangeRates()).willThrow(new IllegalStateException("naver down"));

        assertThatCode(() -> scheduler.broadcastGlobalMarket()).doesNotThrowAnyException();

        verifyNoInteractions(snapshotRepository, publisher);
    }
}
