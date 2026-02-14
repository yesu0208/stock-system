package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.TradePriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisTradePriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisTradePriceRepository;
import arile.toy.stocksystem.stockserver.market.phase.MarketPhaseService;
import arile.toy.stocksystem.stockserver.trading.service.AutoOrderTriggerService;
import arile.toy.stocksystem.stockserver.trading.service.TradeMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradePriceTickMessageHandler {

    private final RedisTradePriceEventPublisher redisTradePriceEventPublisher;
    private final StockServerRedisTradePriceRepository stockServerTradePriceRepository;
    private final TradeMatchingService tradeMatchingService;
    private final AutoOrderTriggerService  autoOrderTriggerService;
    private final MarketPhaseService marketPhaseService;

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
        int fieldSize = 46;

        for (int i = 0; i < count; i++) {
            offset = i * fieldSize;

            String stockCode = fields[offset];

            TradePriceTickMessage tradePriceTickMessage = new TradePriceTickMessage(
                    TickMessageType.TRADEPRICE,
                    stockCode,
                    fields[offset + 1],
                    Integer.parseInt(fields[offset + 2]),
                    Integer.parseInt(fields[offset + 4]),
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

            stockServerTradePriceRepository.save(tradePriceTickMessage);
            redisTradePriceEventPublisher.publish(
                    TradePriceTickEvent.fromMessage(tradePriceTickMessage));

            autoOrderTriggerService.getExternalTickMessageAndTrigger(tradePriceTickMessage);
            tradeMatchingService.getExternalTickMessageAndTrade(tradePriceTickMessage);

            marketPhaseService.closeMarketAfterClosingCall(tradePriceTickMessage.stockCode(),
                    tradePriceTickMessage.tradeTime());
        }
    }
}
