package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.StockSummaryTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisStockSummaryEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.message.StockSummaryTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.repository.StockServerRedisStockSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSummaryTickMessageHandler {

    private final RedisStockSummaryEventPublisher redisStockSummaryEventPublisher;
    private final StockServerRedisStockSummaryRepository stockServerStockSummaryRepository;

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

            StockSummaryTickMessage stockSummaryTickMessage = new StockSummaryTickMessage(
                    stockCode,
                    Integer.parseInt(fields[offset + 2]),
                    Integer.parseInt(fields[offset + 4])
            );

            stockServerStockSummaryRepository.save(stockSummaryTickMessage);
            redisStockSummaryEventPublisher.publish(
                    StockSummaryTickEvent.fromMessage(stockSummaryTickMessage));
        }
    }
}
