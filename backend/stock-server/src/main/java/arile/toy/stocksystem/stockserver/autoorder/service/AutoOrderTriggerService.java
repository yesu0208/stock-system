package arile.toy.stocksystem.stockserver.autoorder.service;

import arile.toy.stocksystem.stockserver.autoorder.dto.*;
import arile.toy.stocksystem.stockserver.autoorder.event.publisher.AutoOrderResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AutoStockLockRegistry;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
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
    private final ReserveAmountCalculator reserveAmountCalculator;

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

            if (!triggerAndRegisterOrder(autoOrderDto)) break;
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

            if (!triggerAndRegisterOrder(autoOrderDto)) break;
        }
    }

    /** @return 다음 자동주문 처리를 계속해도 되면 true, 이번 틱 처리를 멈춰야 하면 false */
    private boolean triggerAndRegisterOrder(AutoOrderDto autoOrderDto) {

        UpdateAutoOrderStatusResult result;
        try {
            result = autoOrderService.updateAutoOrderStatusByTrigger(autoOrderDto.autoOrderId());
        } catch (Exception e) {
            // 발동 상태 변경 실패(롤백): 꺼낸 자동주문을 대기열로 되돌리고 이번 틱 처리 중단
            // (계속 진행하면 되돌린 주문을 다시 꺼내 같은 실패를 반복함)
            log.error("Auto order trigger status update failed. autoOrderId={}", autoOrderDto.autoOrderId(), e);
            autoOrderQueueRegistry.autoOrderEnqueue(autoOrderDto);
            return false;
        }

        if (result.previousStatus() != AutoOrderStatus.ACTIVE) {
            return true;
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromAutoOrderDto(autoOrderDto);

        try {
            orderService.registerOrder(event, true);
        } catch (Exception e) {

            log.error("Auto order trigger -> order registration failed. autoOrderId={}, username={}, stockCode={}",
                    autoOrderDto.autoOrderId(), autoOrderDto.username(), autoOrderDto.stockCode(), e);

            compensateFailedTrigger(autoOrderDto);
            return true;
        }

        // 주문 등록 성공: 예약금은 주문으로 넘어갔으므로, 이후 부가 작업이 실패해도 보상(환불)하면 안 됨
        try {
            stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());
        } catch (Exception e) {
            log.warn("Auto order response delete failed after trigger. autoOrderId={}", autoOrderDto.autoOrderId(), e);
        }
        autoOrderResponseEventPublisher.publishTrigger(autoOrderDto.username());
        return true;
    }

    private void compensateFailedTrigger(AutoOrderDto autoOrderDto) {

        boolean refunded;

        if (autoOrderDto.autoOrderType() == AutoOrderType.BUY) {
            long orderAmount = (long) autoOrderDto.orderPrice() * autoOrderDto.orderQuantity();
            long refundAmount = reserveAmountCalculator.calculateReserveAmount(autoOrderDto.leverageRatio(), orderAmount);
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

        try {
            stockServerAutoOrderResponseRepository.delete(autoOrderDto.username(), autoOrderDto.autoOrderId());
        } catch (Exception e) {
            log.warn("Auto order response delete failed after trigger compensation. autoOrderId={}",
                    autoOrderDto.autoOrderId(), e);
        }

        autoOrderResponseEventPublisher.publishTriggerFailure(autoOrderDto, AutoOrderResultCode.TRIGGER_FAILED);
    }
}
