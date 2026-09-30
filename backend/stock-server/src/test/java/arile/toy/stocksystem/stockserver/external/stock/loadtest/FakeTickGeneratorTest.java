package arile.toy.stocksystem.stockserver.external.stock.loadtest;

import arile.toy.stocksystem.stockserver.alert.service.AlertTriggerService;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderTriggerService;
import arile.toy.stocksystem.stockserver.chart.service.LiveDailyCandleService;
import arile.toy.stocksystem.stockserver.chart.service.LiveMinuteCandleService;
import arile.toy.stocksystem.stockserver.external.stock.dispatcher.ExternalStockTickMessageDispatcher;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisTradePriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.handler.TradePriceTickMessageHandler;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisTradePriceRepository;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoEntryTriggerService;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoExitTriggerService;
import arile.toy.stocksystem.stockserver.trade.service.TradeMatchingService;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopTriggerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("[LoadTest] 가짜 체결가 틱 생성 테스트")
@ExtendWith(MockitoExtension.class)
class FakeTickGeneratorTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @InjectMocks private TradePriceTickMessageHandler handler;

    @Mock private RedisTradePriceEventPublisher redisTradePriceEventPublisher;
    @Mock private StockServerRedisTradePriceRepository stockServerTradePriceRepository;
    @Mock private TradeMatchingService tradeMatchingService;
    @Mock private AutoOrderTriggerService autoOrderTriggerService;
    @Mock private TrailingStopTriggerService trailingStopTriggerService;
    @Mock private OtocoEntryTriggerService otocoEntryTriggerService;
    @Mock private OtocoExitTriggerService otocoExitTriggerService;
    @Mock private AlertTriggerService alertTriggerService;
    @Mock private MarketPhaseService marketPhaseService;
    @Mock private LiveDailyCandleService liveDailyCandleService;
    @Mock private LiveMinuteCandleService liveMinuteCandleService;

    @Mock private ExternalStockTickMessageDispatcher dispatcher;

    @DisplayName("생성한 메시지는 실제 체결가 파서로 해석되며, 매도 체결(5) 틱으로 매칭에 전달된다")
    @Test
    void givenBuiltMessage_whenParsedByRealHandler_thenSellExecutedTick() {
        var sut = generator(List.of("005930"), "2026-09-30T01:00:00Z");

        handler.handle(sut.buildMessage("005930", 995, "100000"));
        handler.handle(sut.buildMessage("005930", 1000, "100001"));

        ArgumentCaptor<TradePriceTickMessage> captor = ArgumentCaptor.forClass(TradePriceTickMessage.class);
        then(tradeMatchingService).should(times(2)).getExternalTickMessageAndTrade(captor.capture());
        TradePriceTickMessage first = captor.getAllValues().get(0);
        TradePriceTickMessage second = captor.getAllValues().get(1);

        assertThat(first.stockCode()).isEqualTo("005930");
        assertThat(first.tradeTime()).isEqualTo("100000");
        assertThat(first.curPrice()).isEqualTo(995);
        assertThat(first.tradingVolumeTick()).isEqualTo(20);
        assertThat(first.tradingType()).isEqualTo(FakeTickGenerator.SELL_EXECUTED);
        assertThat(second.totalTradingVolume()).isEqualTo(40);
    }

    @DisplayName("현재 시각을 체결 시각으로 쓰되, 종가 단일가 구간이면 장 마감으로 처리되지 않도록 대체 시각을 쓴다")
    @Test
    void givenClock_whenResolvingTradeTime_thenAvoidsClosingCallWindow() {
        assertThat(generator(List.of(), "2026-09-30T01:00:00Z").tradeTime()).isEqualTo("100000");
        assertThat(generator(List.of(), "2026-09-30T06:29:50Z").tradeTime()).isEqualTo(FakeTickGenerator.SAFE_TRADE_TIME);
        assertThat(generator(List.of(), "2026-09-30T06:37:49Z").tradeTime()).isEqualTo(FakeTickGenerator.SAFE_TRADE_TIME);
        assertThat(generator(List.of(), "2026-09-30T06:37:50Z").tradeTime()).isEqualTo("153750");
    }

    @DisplayName("한 번 실행하면 설정한 종목마다 가격 범위 안의 틱을 하나씩 디스패처에 넣는다")
    @Test
    void whenGenerating_thenDispatchesOneTickPerStockWithinPriceRange() {
        var sut = generator(List.of("005930", "000660"), "2026-09-30T01:00:00Z");

        sut.generate();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(dispatcher).should(times(2)).dispatch(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(message -> {
            String[] fields = message.split("\\|", 4)[3].split("\\^");
            int price = Integer.parseInt(fields[2]);
            assertThat(message).startsWith("0|H0STCNT0|1|");
            assertThat(price).isBetween(990, 1000);
        });
        assertThat(captor.getAllValues().get(0)).contains("005930^");
        assertThat(captor.getAllValues().get(1)).contains("000660^");
    }

    private FakeTickGenerator generator(List<String> stockCodes, String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), SEOUL);
        return new FakeTickGenerator(dispatcher, stockCodes, 990, 1000, 20, clock);
    }
}
