package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
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
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;

    public void getExternalTickMessageAndTrade(TradePriceTickMessage tradePriceTickMessage) {
        ReentrantLock lock = stockLockRegistry.lock(tradePriceTickMessage.stockCode());

        lock.lock();
        try {
            matchAndExecuteWithinLock(tradePriceTickMessage);
        } finally {
            lock.unlock();
        }
    }

    private void matchAndExecuteWithinLock(TradePriceTickMessage tick) {

        String stockCode = tick.stockCode();
        int tradePrice = tick.curPrice();
        int leftQuantity = tick.tradingVolumeTick();
        String tradingType = tick.tradingType();

        switch (tradingType) {
            case "1" -> matchAndExecuteSellSide(stockCode, tradePrice, leftQuantity);
            case "5" -> matchAndExecuteBuySide(stockCode, tradePrice, leftQuantity);
            case "3", "" -> matchAndExecuteCallAuction(stockCode, tradePrice, leftQuantity);
        }
    }

    private void matchAndExecuteSellSide(String stockCode, int tradePrice, int leftQuantity) {

        while (leftQuantity > 0) {

            var sell = orderQueueRegistry.pollSell(stockCode);

            if (sell == null) return;

            if (sell.orderPrice() > tradePrice) {
                orderQueueRegistry.orderEnqueue(sell);
                return;
            }

            int executable = Math.min(leftQuantity, sell.remainingQuantity());

            var tradeResult = tradeExecutionService.executeSellTrade(sell, tradePrice, executable);

            if (tradeResult == null) {
                log.info("skip canceled order.");
                continue;
            }

            int remaining = sell.remainingQuantity() - executable;
            finalizeOrderAfterExecution(sell, remaining);

            leftQuantity -= executable;
        }
    }

    private void matchAndExecuteBuySide(String stockCode, int tradePrice, int leftQuantity) {

        while (leftQuantity > 0) {

            var buy = orderQueueRegistry.pollBuy(stockCode);

            if (buy == null) return;

            if (buy.orderPrice() < tradePrice) {
                orderQueueRegistry.orderEnqueue(buy);
                return;
            }

            int executable = Math.min(leftQuantity, buy.remainingQuantity());

            var tradeResult = tradeExecutionService.executeBuyTrade(buy, tradePrice, executable);

            if (tradeResult == null) {
                log.info("skip canceled order.");
                continue;
            }

            int remaining = buy.remainingQuantity() - executable;
            finalizeOrderAfterExecution(buy, remaining);

            leftQuantity -= executable;
        }
    }

    private void matchAndExecuteCallAuction(String stockCode, int tradePrice, int leftQuantity) {

        while (leftQuantity > 0) {

            var buy = orderQueueRegistry.pollBuy(stockCode);
            var sell = orderQueueRegistry.pollSell(stockCode);

            if (buy == null || sell == null) {
                if (buy != null) orderQueueRegistry.orderEnqueue(buy);
                if (sell != null) orderQueueRegistry.orderEnqueue(sell);
                break;
            }

            if (buy.orderPrice() < tradePrice || sell.orderPrice() > tradePrice) {
                orderQueueRegistry.orderEnqueue(buy);
                orderQueueRegistry.orderEnqueue(sell);
                break;
            }

            int executable = Math.min(
                    leftQuantity,
                    Math.min(buy.remainingQuantity(), sell.remainingQuantity())
            );

            var sellResult = tradeExecutionService.executeSellTrade(sell, tradePrice, executable);

            if (sellResult != null) {
                int remaining = sell.remainingQuantity() - executable;
                finalizeOrderAfterExecution(sell, remaining);
            } else {
                log.info("skip canceled order.");
            }

            var buyResult = tradeExecutionService.executeBuyTrade(buy, tradePrice, executable);

            if (buyResult != null) {
                int remaining = buy.remainingQuantity() - executable;
                finalizeOrderAfterExecution(buy, remaining);
            } else {
                log.info("skip canceled order.");
            }

            leftQuantity -= executable;
        }
    }

    private void finalizeOrderAfterExecution(OrderDto order, int remainingQuantity) {

        if (remainingQuantity > 0) {

            orderQueueRegistry.orderEnqueue(
                    new OrderDto(
                            order.orderId(),
                            order.username(),
                            order.stockCode(),
                            order.orderType(),
                            order.orderPrice(),
                            order.orderQuantity(),
                            remainingQuantity,
                            OrderStatus.PARTIAL,
                            order.orderTime()
                    )
            );
        }

        if (remainingQuantity == 0) {
            stockServerOrderResponseRepository.delete(
                    order.username(),
                    order.orderId()
            );
        } else {
            stockServerOrderResponseRepository.update(
                    order.username(),
                    order.orderId(),
                    StockServerOrderResponseMessage.of(
                            order.orderId(),
                            order.username(),
                            order.stockCode(),
                            order.orderType(),
                            order.orderPrice(),
                            order.orderQuantity(),
                            remainingQuantity,
                            order.orderTime()
                    )
            );
        }
    }
}
