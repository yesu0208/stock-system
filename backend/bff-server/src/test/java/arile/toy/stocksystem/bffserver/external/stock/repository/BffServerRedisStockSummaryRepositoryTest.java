package arile.toy.stocksystem.bffserver.external.stock.repository;

import arile.toy.stocksystem.bffserver.external.stock.message.BffServerBidAskPriceTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerStockSummaryTickMessage;
import arile.toy.stocksystem.bffserver.external.stock.message.BffServerTradePriceTickMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BffServerRedisStockPriceRepositoriesTest {

    @Nested
    @DisplayName("체결가 저장소")
    class TradePrice {

        @Test
        @DisplayName("trade:{종목코드} 키에서 체결가를 조회하고, 없으면 null을 반환한다")
        @SuppressWarnings("unchecked")
        void findByStockCode() {
            RedisTemplate<String, BffServerTradePriceTickMessage> redisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, BffServerTradePriceTickMessage> valueOps = mock(ValueOperations.class);
            doReturn(valueOps).when(redisTemplate).opsForValue();
            BffServerTradePriceTickMessage trade = mock(BffServerTradePriceTickMessage.class);
            given(valueOps.get("trade:005930")).willReturn(trade);

            BffServerRedisTradePriceRepository repository = new BffServerRedisTradePriceRepository(redisTemplate);

            assertThat(repository.findByStockCode("005930")).isSameAs(trade);
            assertThat(repository.findByStockCode("000660")).isNull();
        }
    }

    @Nested
    @DisplayName("호가 저장소")
    class BidAskPrice {

        @Test
        @DisplayName("bidask:{종목코드} 키에서 호가를 조회하고, 없으면 null을 반환한다")
        @SuppressWarnings("unchecked")
        void findByStockCode() {
            RedisTemplate<String, BffServerBidAskPriceTickMessage> redisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, BffServerBidAskPriceTickMessage> valueOps = mock(ValueOperations.class);
            doReturn(valueOps).when(redisTemplate).opsForValue();
            BffServerBidAskPriceTickMessage bidAsk = mock(BffServerBidAskPriceTickMessage.class);
            given(valueOps.get("bidask:005930")).willReturn(bidAsk);

            BffServerRedisBidAskPriceRepository repository = new BffServerRedisBidAskPriceRepository(redisTemplate);

            assertThat(repository.findByStockCode("005930")).isSameAs(bidAsk);
            assertThat(repository.findByStockCode("000660")).isNull();
        }
    }

    @Nested
    @DisplayName("시세 요약 저장소")
    class StockSummary {

        @Test
        @DisplayName("stock:summary 해시에서 종목코드로 시세 요약을 조회하고, 없으면 null을 반환한다")
        @SuppressWarnings("unchecked")
        void findByStockCode() {
            RedisTemplate<String, BffServerStockSummaryTickMessage> redisTemplate = mock(RedisTemplate.class);
            HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
            doReturn(hashOps).when(redisTemplate).opsForHash();
            BffServerStockSummaryTickMessage summary = new BffServerStockSummaryTickMessage("005930", 71_000, 1_000);
            given(hashOps.get("stock:summary", "005930")).willReturn(summary);

            BffServerRedisStockSummaryRepository repository = new BffServerRedisStockSummaryRepository(redisTemplate);

            assertThat(repository.findByStockCode("005930")).isEqualTo(summary);
            assertThat(repository.findByStockCode("000660")).isNull();
        }
    }
}
