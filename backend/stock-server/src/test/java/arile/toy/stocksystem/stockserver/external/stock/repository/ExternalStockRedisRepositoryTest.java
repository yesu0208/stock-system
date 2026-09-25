package arile.toy.stocksystem.stockserver.external.stock.repository;

import arile.toy.stocksystem.stockserver.external.stock.event.PriceLevel;
import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[Repository] 시세 Redis 저장소 테스트")
@ExtendWith(MockitoExtension.class)
class ExternalStockRedisRepositoryTest {

    @Mock private RedisTemplate<String, TradePriceTickMessage> tradeTemplate;
    @Mock private RedisTemplate<String, BidAskPriceTickMessage> bidAskTemplate;
    @Mock private RedisTemplate<String, StockSummaryTickMessage> summaryTemplate;
    @Mock private ValueOperations<String, TradePriceTickMessage> tradeOps;
    @Mock private ValueOperations<String, BidAskPriceTickMessage> bidAskOps;
    @Mock private HashOperations<String, Object, Object> summaryOps;

    @DisplayName("체결가는 trade:{종목}, 호가는 bidask:{종목} 키에 최신 값으로 저장한다")
    @Test
    void whenSavingTicks_thenSetsLatestValue() {
        given(tradeTemplate.opsForValue()).willReturn(tradeOps);
        given(bidAskTemplate.opsForValue()).willReturn(bidAskOps);
        var trade = mock(TradePriceTickMessage.class);
        given(trade.stockCode()).willReturn("005930");
        var bidAsk = new BidAskPriceTickMessage(TickMessageType.BIDASKPRICE, "005930",
                List.of(new PriceLevel(70_000, 1)), List.of(new PriceLevel(69_900, 2)), 1, 2);

        new StockServerRedisTradePriceRepository(tradeTemplate).save(trade);
        new StockServerRedisBidAskPriceRepository(bidAskTemplate).save(bidAsk);

        then(tradeOps).should().set("trade:005930", trade);
        then(bidAskOps).should().set("bidask:005930", bidAsk);
    }

    @DisplayName("요약은 stock:summary 해시에 종목별로 저장·조회한다")
    @Test
    void whenSavingSummary_thenPutsHash() {
        willReturn(summaryOps).given(summaryTemplate).opsForHash();
        var summary = new StockSummaryTickMessage("005930", 70_000, 1_000);
        given(summaryOps.get("stock:summary", "005930")).willReturn(summary);
        var sut = new StockServerRedisStockSummaryRepository(summaryTemplate);

        sut.save(summary);

        then(summaryOps).should().put("stock:summary", "005930", summary);
        assertThat(sut.findByStockCode("005930")).isEqualTo(summary);
    }
}
