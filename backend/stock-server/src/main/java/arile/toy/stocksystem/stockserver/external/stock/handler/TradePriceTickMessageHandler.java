package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.alert.service.AlertTriggerService;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderTriggerService;
import arile.toy.stocksystem.stockserver.chart.service.LiveDailyCandleService;
import arile.toy.stocksystem.stockserver.chart.service.LiveMinuteCandleService;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradePriceTickMessageHandler {

    private final RedisTradePriceEventPublisher redisTradePriceEventPublisher;
    private final StockServerRedisTradePriceRepository stockServerTradePriceRepository;
    private final TradeMatchingService tradeMatchingService;
    private final AutoOrderTriggerService autoOrderTriggerService;
    private final TrailingStopTriggerService trailingStopTriggerService;
    private final OtocoEntryTriggerService otocoEntryTriggerService;
    private final OtocoExitTriggerService otocoExitTriggerService;
    private final AlertTriggerService alertTriggerService;
    private final MarketPhaseService marketPhaseService;
    private final LiveDailyCandleService liveDailyCandleService;
    private final LiveMinuteCandleService liveMinuteCandleService;

    private MeterRegistry meterRegistry = Metrics.globalRegistry;

    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void handle(String message) {

        String[] parts = message.split("\\|", 4);

//      String encrypted = parts[0];
        String trId = parts[1];
        int count = Integer.parseInt(parts[2]); // 한 메시지에 여러 개의 데이터 들어있을 수 있다
        String payload = parts[3];

//      복호화(생략)
//      if ("1".equals(encrypted)) {
//          payload = aes256.decrypt(payload, key, iv);
//      }

        String[] fields = payload.split("\\^");
        int offset;
        int fieldSize = 47;

        for (int i = 0; i < count; i++) {
            offset = i * fieldSize;

            String stockCode = fields[offset];

            try {
                Integer curPrice = Integer.parseInt(fields[offset + 2]);
                Integer prevCloseDiff = Integer.parseInt(fields[offset + 4]);
                Integer prevClosePrice = curPrice - prevCloseDiff;
                String prevCloseRate = (prevClosePrice != 0)
                        ? String.format("%.2f", (prevCloseDiff * 100.0) / prevClosePrice)
                        : "0.00";

                TradePriceTickMessage tradePriceTickMessage = new TradePriceTickMessage(
                        TickMessageType.TRADEPRICE,
                        stockCode,
                        fields[offset + 1],
                        curPrice,
                        prevCloseDiff,
                        prevClosePrice,
                        prevCloseRate,
                        Integer.parseInt(fields[offset + 7]),
                        Integer.parseInt(fields[offset + 8]),
                        Integer.parseInt(fields[offset + 9]),
                        Integer.parseInt(fields[offset + 12]),
                        Integer.parseInt(fields[offset + 13]),
                        Long.parseLong(fields[offset + 14]),
                        Integer.parseInt(fields[offset + 19]),
                        Integer.parseInt(fields[offset + 20]),
                        fields[offset + 21],
                        Integer.parseInt(fields[offset + 41])
                );

                // 단계별로 격리: 앞 단계(예: Redis 저장, 자동주문 발동)가 실패해도
                // 같은 틱의 체결 매칭·장 마감 전환 등 나머지 단계는 반드시 수행되어야 함
                // 틱 1건의 전체 처리 시간 (11단계 합계)
                Timer.Sample tickSample = Timer.start(meterRegistry);
                runStep(stockCode, "saveTradePrice", () -> stockServerTradePriceRepository.save(tradePriceTickMessage));
                runStep(stockCode, "publishTradePrice", () -> redisTradePriceEventPublisher.publish(
                        TradePriceTickEvent.fromMessage(tradePriceTickMessage)));
                runStep(stockCode, "autoOrder", () -> autoOrderTriggerService.getExternalTickMessageAndTrigger(tradePriceTickMessage));
                runStep(stockCode, "trailingStop", () -> trailingStopTriggerService.getExternalTickMessageAndTrail(tradePriceTickMessage));
                runStep(stockCode, "otocoEntry", () -> otocoEntryTriggerService.getExternalTickMessageAndTriggerEntry(tradePriceTickMessage));
                runStep(stockCode, "otocoExit", () -> otocoExitTriggerService.getExternalTickMessageAndSettleExit(tradePriceTickMessage));
                runStep(stockCode, "tradeMatching", () -> tradeMatchingService.getExternalTickMessageAndTrade(tradePriceTickMessage));
                runStep(stockCode, "alert", () -> alertTriggerService.getExternalTickMessageAndCheckAlerts(tradePriceTickMessage));
                runStep(stockCode, "closingCall", () -> marketPhaseService.closeMarketAfterClosingCall(
                        tradePriceTickMessage.stockCode(), tradePriceTickMessage.tradeTime()));
                runStep(stockCode, "dailyCandle", () -> liveDailyCandleService.buildAndPublish(stockCode, tradePriceTickMessage));
                runStep(stockCode, "minuteCandle", () -> liveMinuteCandleService.updateAndPublish(stockCode, tradePriceTickMessage));
                tickSample.stop(timer("tick.process", null));
            } catch (NumberFormatException e) {
                log.warn("[TICK 파싱 실패] 소수점 등 처리 불가 가격 데이터 무시. stockCode={}, message={}",
                        stockCode, e.getMessage());
            }
        }
    }

    private void runStep(String stockCode, String step, Runnable action) {
        // 단계별 처리 시간 (실패해도 걸린 시간은 기록)
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            action.run();
        } catch (Exception e) {
            log.error("Trade price tick step failed. stockCode={}, step={}", stockCode, step, e);
        } finally {
            sample.stop(timer("tick.step", step));
        }
    }

    // 종목 코드는 태그로 쓰지 않음 (종목 수만큼 시계열이 늘어나므로 단계 이름만 구분)
    private Timer timer(String name, String step) {
        Timer.Builder builder = Timer.builder(name).publishPercentileHistogram();
        if (step != null) {
            builder.tag("step", step);
        }
        return builder.register(meterRegistry);
    }
}
