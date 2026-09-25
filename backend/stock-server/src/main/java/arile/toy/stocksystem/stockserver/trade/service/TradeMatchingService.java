package arile.toy.stocksystem.stockserver.trade.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.dto.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.order.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.order.service.QueuePositionBroadcastService;
import arile.toy.stocksystem.stockserver.trade.dto.TradeResult;
import arile.toy.stocksystem.stockserver.trade.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.trade.event.publisher.TradeResponseEventPublisher;
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
    private final TradeResponseEventPublisher tradeResponseEventPublisher;
    private final QueuePositionBroadcastService queuePositionBroadcastService;

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

            TradeResult tradeResult;
            try {
                tradeResult = tradeExecutionService.executeSellTrade(sell, tradePrice, executable);
            } catch (Exception e) {
                // 체결 트랜잭션 실패(롤백): 꺼낸 주문을 대기열로 되돌리고 이번 틱 매칭 중단
                log.error("Sell trade execution failed. orderId={}", sell.orderId(), e);
                orderQueueRegistry.orderEnqueue(sell);
                return;
            }

            if (tradeResult == null) {
                log.info("skip canceled order.");
                continue;
            }

            tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(tradeResult.tradeEntity()));

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

            TradeResult tradeResult;
            try {
                tradeResult = tradeExecutionService.executeBuyTrade(buy, tradePrice, executable);
            } catch (Exception e) {
                // 체결 트랜잭션 실패(롤백): 꺼낸 주문을 대기열로 되돌리고 이번 틱 매칭 중단
                log.error("Buy trade execution failed. orderId={}", buy.orderId(), e);
                orderQueueRegistry.orderEnqueue(buy);
                return;
            }

            if (tradeResult == null) {
                log.info("skip canceled order.");
                continue;
            }

            tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(tradeResult.tradeEntity()));

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

            TradeResult sellResult;
            try {
                sellResult = tradeExecutionService.executeSellTrade(sell, tradePrice, executable);
            } catch (Exception e) {
                // 매도 체결 실패(롤백): 아직 체결 전인 매수·매도 모두 대기열로 되돌리고 중단
                log.error("Call auction sell execution failed. orderId={}", sell.orderId(), e);
                orderQueueRegistry.orderEnqueue(buy);
                orderQueueRegistry.orderEnqueue(sell);
                break;
            }

            if (sellResult != null) {
                tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(sellResult.tradeEntity()));
                int remaining = sell.remainingQuantity() - executable;
                finalizeOrderAfterExecution(sell, remaining);
            } else {
                log.info("skip canceled order.");
            }

            TradeResult buyResult;
            try {
                buyResult = tradeExecutionService.executeBuyTrade(buy, tradePrice, executable);
            } catch (Exception e) {
                // 매수 체결 실패(롤백): 매도는 이미 처리됐으므로 매수만 대기열로 되돌리고 중단
                log.error("Call auction buy execution failed. orderId={}", buy.orderId(), e);
                orderQueueRegistry.orderEnqueue(buy);
                break;
            }

            if (buyResult != null) {
                tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(buyResult.tradeEntity()));
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
                            order.leverageRatio(),
                            order.orderPrice(),
                            order.orderQuantity(),
                            remainingQuantity,
                            OrderStatus.PARTIAL,
                            order.orderTime(),
                            order.orderExecutionType(),
                            order.origin(),
                            order.originId()
                    )
            );
        }

        // 이하 체결 커밋 이후의 부가 작업: 실패해도 매칭을 중단하지 않음 (체결 결과는 이미 DB에 반영됨)
        try {
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
                                order.leverageRatio(),
                                order.orderPrice(),
                                order.orderQuantity(),
                                remainingQuantity,
                                order.orderTime(),
                                order.orderExecutionType(),
                                order.origin(),
                                order.originId()
                        )
                );
            }
        } catch (Exception e) {
            log.warn("Order response update failed after trade. orderId={}", order.orderId(), e);
        }

        try {
            queuePositionBroadcastService.broadcast(order.stockCode(), order.orderType());
        } catch (Exception e) {
            log.warn("Queue position broadcast failed after trade. orderId={}", order.orderId(), e);
        }
    }
}
