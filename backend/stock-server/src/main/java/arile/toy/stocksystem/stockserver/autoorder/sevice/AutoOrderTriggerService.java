package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AutoStockLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoOrderTriggerService {

    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoStockLockRegistry autoStockLockRegistry;
    private final AutoOrderService autoOrderService;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final OrderService orderService;
    private final AutoOrderResponseEventPublisher autoOrderResponseEventPublisher;
    private final AccountApiClient accountApiClient;

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

            triggerAndRegisterOrder(autoOrderDto);
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

            triggerAndRegisterOrder(autoOrderDto);
        }
    }

    private void triggerAndRegisterOrder(AutoOrderDto autoOrderDto) {

        UpdateAutoOrderStatusResult result =
                autoOrderService.updateAutoOrderStatusByTrigger(autoOrderDto.autoOrderId());

        if (result.previousStatus() != AutoOrderStatus.ACTIVE) {
            return;
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromAutoOrderDto(autoOrderDto);

        try {
            orderService.registerOrder(event, true);

            stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());
            autoOrderResponseEventPublisher.publishTrigger(autoOrderDto.username());

        } catch (Exception e) {

            log.error("Auto order trigger -> order registration failed. autoOrderId={}, username={}, stockCode={}",
                    autoOrderDto.autoOrderId(), autoOrderDto.username(), autoOrderDto.stockCode(), e);

            compensateFailedTrigger(autoOrderDto);
        }
    }

    private void compensateFailedTrigger(AutoOrderDto autoOrderDto) {

        boolean refunded;

        if (autoOrderDto.autoOrderType() == AutoOrderType.BUY) {
            long orderAmount = (long) autoOrderDto.orderPrice() * autoOrderDto.orderQuantity();
            long refundAmount = autoOrderDto.leverageRatio().isSpot()
                    ? orderAmount
                    : autoOrderDto.leverageRatio().calculateMarginDeposit(orderAmount);
            refunded = accountApiClient.refundReservedCash(autoOrderDto.username(), refundAmount);
        } else {
            refunded = autoOrderDto.leverageRatio().isSpot()
                    ? accountApiClient.refundReservedStock(autoOrderDto.username(), autoOrderDto.stockCode(), autoOrderDto.orderQuantity())
                    : accountApiClient.refundReservedLeverageStock(autoOrderDto.username(), autoOrderDto.stockCode(),
                    autoOrderDto.leverageRatio().name(), autoOrderDto.orderQuantity());
        }

        if (!refunded) {
            log.error("CRITICAL: Auto order trigger compensation refund FAILED. " +
                            "Manual intervention required. autoOrderId={}, username={}, stockCode={}, type={}",
                    autoOrderDto.autoOrderId(), autoOrderDto.username(),
                    autoOrderDto.stockCode(), autoOrderDto.autoOrderType());
        }

        stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());

        autoOrderResponseEventPublisher.publishTriggerFailure(autoOrderDto, AutoOrderResultCode.INTERNAL_ERROR);
    }
}
