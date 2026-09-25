package arile.toy.stocksystem.stockserver.external.stock.event.publisher;

import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("[Publisher] 시세 이벤트 발행 테스트")
@ExtendWith(MockitoExtension.class)
class ExternalStockEventPublisherTest {

    @Mock private RedisTemplate<String, TradePriceTickEvent> tradeTemplate;
    @Mock private RedisTemplate<String, BidAskPriceTickEvent> bidAskTemplate;
    @Mock private RedisTemplate<String, StockSummaryTickEvent> summaryTemplate;

    @DisplayName("체결가·호가는 종목별 채널, 요약은 공통 채널로 발행한다")
    @Test
    void whenPublishing_thenSendsToChannels() {
        new RedisTradePriceEventPublisher(tradeTemplate).publish(new TradePriceTickEvent("005930"));
        new RedisBidAskPriceEventPublisher(bidAskTemplate).publish(new BidAskPriceTickEvent("005930"));
        new RedisStockSummaryEventPublisher(summaryTemplate).publish(new StockSummaryTickEvent("005930"));

        then(tradeTemplate).should().convertAndSend("trade.005930:event", new TradePriceTickEvent("005930"));
        then(bidAskTemplate).should().convertAndSend("bidask.005930:event", new BidAskPriceTickEvent("005930"));
        then(summaryTemplate).should().convertAndSend("summary:event", new StockSummaryTickEvent("005930"));
    }

    @DisplayName("Redis 발행이 실패해도 예외를 전파하지 않는다")
    @Test
    void givenRedisFails_whenPublishing_thenSwallows() {
        given(tradeTemplate.convertAndSend(anyString(), any(TradePriceTickEvent.class))).willThrow(new IllegalStateException());
        given(bidAskTemplate.convertAndSend(anyString(), any(BidAskPriceTickEvent.class))).willThrow(new IllegalStateException());
        given(summaryTemplate.convertAndSend(anyString(), any(StockSummaryTickEvent.class))).willThrow(new IllegalStateException());

        assertThatNoException().isThrownBy(() -> {
            new RedisTradePriceEventPublisher(tradeTemplate).publish(new TradePriceTickEvent("005930"));
            new RedisBidAskPriceEventPublisher(bidAskTemplate).publish(new BidAskPriceTickEvent("005930"));
            new RedisStockSummaryEventPublisher(summaryTemplate).publish(new StockSummaryTickEvent("005930"));
        });
    }
}
