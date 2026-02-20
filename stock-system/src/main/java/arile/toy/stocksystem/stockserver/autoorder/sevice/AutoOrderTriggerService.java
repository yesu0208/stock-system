package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderStatus;
import arile.toy.stocksystem.stockserver.autoorder.dto.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AutoStockLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class AutoOrderTriggerService {

    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoStockLockRegistry autoStockLockRegistry;
    private final AutoOrderService autoOrderService;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final OrderService orderService;
    private final AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;

    public void getExternalTickMessageAndTrigger(TradePriceTickMessage tradePriceTickMessage) {
        ReentrantLock lock = autoStockLockRegistry.lock(tradePriceTickMessage.stockCode());

        lock.lock();
        try {
            moveToStockQueue(tradePriceTickMessage);
        } finally {
            lock.unlock();
        }

    }

    private void moveToStockQueue(TradePriceTickMessage tick) {
        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        pollAndTriggerSell(stockCode, currentPrice);

        pollAndTriggerBuy(stockCode, currentPrice);
    }

    private void pollAndTriggerSell(String stockCode, int currentPrice) {
        while (true) {
            AutoOrderDto autoOrderDto = autoOrderQueueRegistry.pollSell(stockCode);

            if (autoOrderDto == null) break;

            if (autoOrderDto.triggerPrice() < currentPrice) {
                autoOrderQueueRegistry.autoOrderEnqueue(autoOrderDto);
                break;
            }

            UpdateAutoOrderStatusResult result =
                    autoOrderService.updateAutoOrderStatusByTrigger(autoOrderDto.autoOrderId());
            if (result.previousStatus() != AutoOrderStatus.ACTIVE) {
                continue;
            }

            StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromAutoOrderDto(autoOrderDto);
            orderService.registerOrder(event, true);

            stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());
            autoOrderResponseEventPublisher.publishTrigger(autoOrderDto.username());
        }
    }

    private void pollAndTriggerBuy(String stockCode, int currentPrice) {
        while (true) {
            AutoOrderDto autoOrderDto = autoOrderQueueRegistry.pollBuy(stockCode);

            if (autoOrderDto == null) break;

            if (autoOrderDto.triggerPrice() > currentPrice) {
                autoOrderQueueRegistry.autoOrderEnqueue(autoOrderDto);
                break;
            }

            UpdateAutoOrderStatusResult result =
                    autoOrderService.updateAutoOrderStatusByTrigger(autoOrderDto.autoOrderId());
            if (result.previousStatus() != AutoOrderStatus.ACTIVE) {
                continue;
            }

            StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromAutoOrderDto(autoOrderDto);
            orderService.registerOrder(event, true);

            stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());
            autoOrderResponseEventPublisher.publishTrigger(autoOrderDto.username());
        }
    }
}
