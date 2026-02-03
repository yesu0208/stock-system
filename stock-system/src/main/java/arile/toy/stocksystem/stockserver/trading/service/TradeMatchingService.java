package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.order.OrderDto;
import arile.toy.stocksystem.stockserver.trading.dto.order.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.order.OrderStatus;
import arile.toy.stocksystem.stockserver.trading.dto.order.StockServerOrderResponseMessage;
import arile.toy.stocksystem.stockserver.trading.event.TradeResponseEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.RedisOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.event.publisher.RedisTradeResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.event.publisher.TradeResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerOrderResponseRepository;
import arile.toy.stocksystem.stockserver.trading.repository.TradeCommand;
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
    private final TradeResponseEventPublisher tradeResponseEventPublisher;
    private final StockServerOrderResponseRepository stockServerOrderResponseRepository;
    private final TradeCommand tradeCommand;

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

                var tradeResult = tradeExecutionService.executeSellTrade(sellOrderDto, tradePrice, executable);

                if (tradeResult != null) {

                    int remainingQuantity = sellOrderDto.remainingQuantity() - executable;

                    if (remainingQuantity > 0) {
                        var reEnqueuedOrderDto = new OrderDto(sellOrderDto.orderId(),
                                sellOrderDto.username(),
                                sellOrderDto.stockCode(),
                                sellOrderDto.orderType(),
                                sellOrderDto.orderPrice(),
                                sellOrderDto.orderQuantity(),
                                remainingQuantity,
                                OrderStatus.PARTIAL,
                                sellOrderDto.orderTime());
                        orderQueueRegistry.orderEnqueue(reEnqueuedOrderDto);
                    }

                    if (remainingQuantity == 0) {
                        stockServerOrderResponseRepository.delete(sellOrderDto.username(), sellOrderDto.orderId());
                    } else {
                        stockServerOrderResponseRepository.update(tradeResult.tradeEntity().getUsername(),
                                tradeResult.tradeEntity().getOrderId(),
                                StockServerOrderResponseMessage.of(
                                        sellOrderDto.orderId(),
                                        sellOrderDto.username(),
                                        sellOrderDto.stockCode(),
                                        sellOrderDto.orderType(),
                                        sellOrderDto.orderPrice(),
                                        sellOrderDto.orderQuantity(),
                                        remainingQuantity,
                                        sellOrderDto.orderTime()
                                ));
                    }

                    long tradeAmount = (long) sellOrderDto.orderPrice() * tradeResult.tradeEntity().getTradeQuantity();
                    long differenceAmount = (long) (tradeResult.tradeEntity().getTradePrice() - sellOrderDto.orderPrice()) * tradeResult.tradeEntity().getTradeQuantity();

                    long buyPrice = 0;
                    if (tradeResult.totalQuantity() != 0) {
                        buyPrice = tradeResult.totalAmount() / tradeResult.totalQuantity();
                    }

                    boolean redisOk = tradeCommand.applySellTrade(
                            tradeResult.tradeEntity().getUsername(),
                            tradeResult.tradeEntity().getStockCode(),
                            tradeResult.totalQuantity(),
                            buyPrice,
                            tradeAmount,
                            differenceAmount
                    );

                    if (!redisOk) {
                        log.error("Redis sell command failed.");
                    }

                    tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(tradeResult.tradeEntity()));
                    leftQuantity -= tradeResult.tradeEntity().getTradeQuantity();
                } else {
                    log.debug("skip canceled order.");
                }

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

                var tradeResult = tradeExecutionService.executeBuyTrade(buyOrderDto, tradePrice, executable);

                if (tradeResult != null) {

                    int remainingQuantity = buyOrderDto.remainingQuantity() - executable;

                    if (remainingQuantity > 0) {
                        var reEnqueuedOrderDto = new OrderDto(buyOrderDto.orderId(),
                                buyOrderDto.username(),
                                buyOrderDto.stockCode(),
                                buyOrderDto.orderType(),
                                buyOrderDto.orderPrice(),
                                buyOrderDto.orderQuantity(),
                                remainingQuantity,
                                OrderStatus.PARTIAL,
                                buyOrderDto.orderTime());
                        orderQueueRegistry.orderEnqueue(reEnqueuedOrderDto);
                    }

                    if (remainingQuantity == 0) {
                        stockServerOrderResponseRepository.delete(buyOrderDto.username(), buyOrderDto.orderId());
                    } else {
                        stockServerOrderResponseRepository.update(tradeResult.tradeEntity().getUsername(),
                                tradeResult.tradeEntity().getOrderId(),
                                StockServerOrderResponseMessage.of(
                                        buyOrderDto.orderId(),
                                        buyOrderDto.username(),
                                        buyOrderDto.stockCode(),
                                        buyOrderDto.orderType(),
                                        buyOrderDto.orderPrice(),
                                        buyOrderDto.orderQuantity(),
                                        remainingQuantity,
                                        buyOrderDto.orderTime()
                                ));
                    }

                    long tradeAmount = (long) buyOrderDto.orderPrice() * tradeResult.tradeEntity().getTradeQuantity();
                    long differenceAmount = (long) (buyOrderDto.orderPrice() - tradeResult.tradeEntity().getTradePrice()) * tradeResult.tradeEntity().getTradeQuantity();

                    boolean redisOk = tradeCommand.applySellTrade(
                            tradeResult.tradeEntity().getUsername(),
                            tradeResult.tradeEntity().getStockCode(),
                            tradeResult.totalQuantity(),
                            tradeResult.totalAmount()/tradeResult.totalQuantity(),
                            tradeAmount,
                            differenceAmount
                    );

                    if (!redisOk) {
                        log.error("Redis buy command failed.");
                    }

                    tradeResponseEventPublisher.publish(TradeResponseEvent.fromEntity(tradeResult.tradeEntity()));
                    leftQuantity -= tradeResult.tradeEntity().getTradeQuantity();
                }

            }
        }
    }
}
