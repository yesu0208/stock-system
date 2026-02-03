package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.order.OrderQueueRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeMatchingService {

    private final StockLockRegistry stockLockRegistry;
    private final OrderQueueRegistry orderQueueRegistry;
    private final TradeExecutionService tradeExecutionService;

    public void getExternalTickMessageAndTrade(TradePriceTickMessage tradePriceTickMessage) {
        ReentrantLock lock = stockLockRegistry.lock(tradePriceTickMessage.stockCode());

        lock.lock();
        try {
            matchAndExecuteWithinLock(tradePriceTickMessage);
        } finally {
            lock.unlock();
        }
    }

    private void matchAndExecuteWithinLock(TradePriceTickMessage tradePriceTickMessage) {
        String stockCode = tradePriceTickMessage.stockCode();
        int tradePrice = tradePriceTickMessage.curPrice();
        int leftQuantity = tradePriceTickMessage.tradingVolumeTick();
        String tradingType = tradePriceTickMessage.tradingType(); // 1:BUY, 5:SELL

        if (tradingType.equals("1")) { // BUY -> 모의 투자는 SELL
            while (leftQuantity > 0) {
                var sellOrderDto = orderQueueRegistry.pollSell(stockCode);
                if (sellOrderDto == null) return;

                if (sellOrderDto.orderPrice() > tradePrice) {
                    orderQueueRegistry.orderEnqueue(sellOrderDto);
                    return;
                }

                int executable = Math.min(leftQuantity, sellOrderDto.remainingQuantity());

                tradeExecutionService.executeSellTrade(sellOrderDto, tradePrice, executable);
            }
        } else if (tradingType.equals("5")) { // SELL -> 모의 투자는 BUY
            while (leftQuantity > 0) {
                var buyOrderDto = orderQueueRegistry.pollBuy(stockCode);
                if (buyOrderDto == null) return;

                if (buyOrderDto.orderPrice() < tradePrice) {
                    orderQueueRegistry.orderEnqueue(buyOrderDto);
                    return;
                }

                int executable = Math.min(leftQuantity, buyOrderDto.remainingQuantity());

                tradeExecutionService.executeBuyTrade(buyOrderDto, tradePrice, executable);

            }
        }
    }
}
