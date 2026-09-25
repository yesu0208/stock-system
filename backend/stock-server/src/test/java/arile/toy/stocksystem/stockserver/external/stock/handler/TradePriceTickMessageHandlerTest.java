package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.alert.service.AlertTriggerService;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderTriggerService;
import arile.toy.stocksystem.stockserver.chart.service.LiveDailyCandleService;
import arile.toy.stocksystem.stockserver.chart.service.LiveMinuteCandleService;
import arile.toy.stocksystem.stockserver.external.stock.TickMessages;
import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisTradePriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@DisplayName("[Handler] 체결가 시세 처리 테스트")
@ExtendWith(MockitoExtension.class)
class TradePriceTickMessageHandlerTest {

    @InjectMocks private TradePriceTickMessageHandler sut;

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

    @DisplayName("필드를 파싱해 전일 대비 값을 계산하고 저장·발행 후 발동·체결·장 마감·캔들 순으로 처리한다")
    @Test
    void givenTick_whenHandling_thenParsesAndRunsAllSteps() {
        sut.handle(TickMessages.message("H0STCNT0", TickMessages.tradeRecord("005930", "70000", 1000)));

        ArgumentCaptor<TradePriceTickMessage> captor = ArgumentCaptor.forClass(TradePriceTickMessage.class);
        then(stockServerTradePriceRepository).should().save(captor.capture());
        TradePriceTickMessage tick = captor.getValue();
        assertThat(tick.tickMessageType()).isEqualTo(TickMessageType.TRADEPRICE);
        assertThat(tick.stockCode()).isEqualTo("005930");
        assertThat(tick.tradeTime()).isEqualTo("093000");
        assertThat(tick.curPrice()).isEqualTo(70_000);
        assertThat(tick.prevClosePrice()).isEqualTo(69_000);
        assertThat(tick.prevCloseRate()).isEqualTo("1.45");
        assertThat(tick.highPrice()).isEqualTo(71_000);
        assertThat(tick.totalTradingValue()).isEqualTo(70_000_000L);
        assertThat(tick.tradingType()).isEqualTo("1");
        assertThat(tick.prevDaySameTimeAccVolume()).isEqualTo(900);

        InOrder inOrder = inOrder(redisTradePriceEventPublisher, autoOrderTriggerService, trailingStopTriggerService,
                otocoEntryTriggerService, otocoExitTriggerService, tradeMatchingService, alertTriggerService,
                marketPhaseService, liveDailyCandleService, liveMinuteCandleService);
        inOrder.verify(redisTradePriceEventPublisher).publish(new TradePriceTickEvent("005930"));
        inOrder.verify(autoOrderTriggerService).getExternalTickMessageAndTrigger(tick);
        inOrder.verify(trailingStopTriggerService).getExternalTickMessageAndTrail(tick);
        inOrder.verify(otocoEntryTriggerService).getExternalTickMessageAndTriggerEntry(tick);
        inOrder.verify(otocoExitTriggerService).getExternalTickMessageAndSettleExit(tick);
        inOrder.verify(tradeMatchingService).getExternalTickMessageAndTrade(tick);
        inOrder.verify(alertTriggerService).getExternalTickMessageAndCheckAlerts(tick);
        inOrder.verify(marketPhaseService).closeMarketAfterClosingCall("005930", "093000");
        inOrder.verify(liveDailyCandleService).buildAndPublish("005930", tick);
        inOrder.verify(liveMinuteCandleService).updateAndPublish("005930", tick);
    }

    @DisplayName("전일 종가가 0이면 등락률은 0.00이다")
    @Test
    void givenZeroPrevClose_whenHandling_thenZeroRate() {
        sut.handle(TickMessages.message("H0STCNT0", TickMessages.tradeRecord("005930", "1000", 1000)));

        then(stockServerTradePriceRepository).should().save(argThat(t -> t.prevCloseRate().equals("0.00")));
    }

    @DisplayName("한 단계가 실패해도 나머지 단계(체결 매칭·장 마감 전환 등)는 모두 수행한다")
    @Test
    void givenStepFails_whenHandling_thenRunsRemainingSteps() {
        willThrow(new IllegalStateException("redis down")).given(stockServerTradePriceRepository).save(any());
        willThrow(new IllegalStateException("auto order error"))
                .given(autoOrderTriggerService).getExternalTickMessageAndTrigger(any());

        sut.handle(TickMessages.message("H0STCNT0", TickMessages.tradeRecord("005930", "70000", 1000)));

        then(tradeMatchingService).should().getExternalTickMessageAndTrade(any());
        then(marketPhaseService).should().closeMarketAfterClosingCall(eq("005930"), any());
        then(liveMinuteCandleService).should().updateAndPublish(eq("005930"), any());
    }

    @DisplayName("여러 건 중 파싱할 수 없는 레코드는 건너뛰고 나머지를 처리한다")
    @Test
    void givenMultipleRecords_whenOneInvalid_thenSkipsIt() {
        sut.handle(TickMessages.message("H0STCNT0",
                TickMessages.tradeRecord("005930", "70000.5", 1000),
                TickMessages.tradeRecord("000660", "120000", -500)));

        then(stockServerTradePriceRepository).should().save(argThat(t -> t.stockCode().equals("000660")));
        then(tradeMatchingService).should(times(1)).getExternalTickMessageAndTrade(any());
    }
}
