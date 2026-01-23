package arile.toy.stocksystem.stockserver.external.stock.handler;

import arile.toy.stocksystem.stockserver.external.stock.event.BidAskPriceTickEvent;
import arile.toy.stocksystem.stockserver.external.stock.event.publisher.RedisBidAskPriceEventPublisher;
import arile.toy.stocksystem.stockserver.external.stock.event.PriceLevel;
import arile.toy.stocksystem.stockserver.external.stock.message.BidAskPriceTickMessage;
import arile.toy.stocksystem.stockserver.external.stock.message.TickMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BidAskPriceTickMessageHandler {

    private final RedisBidAskPriceEventPublisher redisBidAskPriceEventPublisher;

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
        int fieldSize = 59;
        int offset;

        for (int i = 0; i < count; i++) {
            offset = i * fieldSize;

            String stockCode = fields[offset];

            BidAskPriceTickMessage bidAskPriceTickMessage = new BidAskPriceTickMessage(
                    TickMessageType.BIDASKPRICE,
                    stockCode,
                    List.of(new PriceLevel(Integer.parseInt(fields[offset + 3]), Integer.parseInt(fields[offset+23])),
                            new PriceLevel(Integer.parseInt(fields[offset + 4]), Integer.parseInt(fields[offset+24])),
                            new PriceLevel(Integer.parseInt(fields[offset + 5]), Integer.parseInt(fields[offset+25])),
                            new PriceLevel(Integer.parseInt(fields[offset + 6]), Integer.parseInt(fields[offset+26])),
                            new PriceLevel(Integer.parseInt(fields[offset + 7]), Integer.parseInt(fields[offset+27])),
                            new PriceLevel(Integer.parseInt(fields[offset + 8]), Integer.parseInt(fields[offset+28])),
                            new PriceLevel(Integer.parseInt(fields[offset + 9]), Integer.parseInt(fields[offset+29])),
                            new PriceLevel(Integer.parseInt(fields[offset + 10]), Integer.parseInt(fields[offset+30])),
                            new PriceLevel(Integer.parseInt(fields[offset + 11]), Integer.parseInt(fields[offset+31])),
                            new PriceLevel(Integer.parseInt(fields[offset + 12]), Integer.parseInt(fields[offset+32]))),
                    List.of(new PriceLevel(Integer.parseInt(fields[offset + 13]), Integer.parseInt(fields[offset+33])),
                            new PriceLevel(Integer.parseInt(fields[offset + 14]), Integer.parseInt(fields[offset+34])),
                            new PriceLevel(Integer.parseInt(fields[offset + 15]), Integer.parseInt(fields[offset+35])),
                            new PriceLevel(Integer.parseInt(fields[offset + 16]), Integer.parseInt(fields[offset+36])),
                            new PriceLevel(Integer.parseInt(fields[offset + 17]), Integer.parseInt(fields[offset+37])),
                            new PriceLevel(Integer.parseInt(fields[offset + 18]), Integer.parseInt(fields[offset+38])),
                            new PriceLevel(Integer.parseInt(fields[offset + 19]), Integer.parseInt(fields[offset+39])),
                            new PriceLevel(Integer.parseInt(fields[offset + 20]), Integer.parseInt(fields[offset+40])),
                            new PriceLevel(Integer.parseInt(fields[offset + 21]), Integer.parseInt(fields[offset+41])),
                            new PriceLevel(Integer.parseInt(fields[offset + 22]), Integer.parseInt(fields[offset+42]))),
                    Integer.parseInt(fields[offset + 43]),
                    Integer.parseInt(fields[offset + 44])
            );

            redisBidAskPriceEventPublisher.publish(
                    BidAskPriceTickEvent.fromMessage(bidAskPriceTickMessage));
        }
    }
}
