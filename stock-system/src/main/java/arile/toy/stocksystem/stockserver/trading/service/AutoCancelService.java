package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.auto.cancel.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.trading.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.trading.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.trading.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.trading.event.publisher.AutoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.trading.repository.AutoCancelRepository;
import arile.toy.stocksystem.stockserver.trading.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.event.publisher.AccountUpdateEventPublisher;
import arile.toy.stocksystem.stockserver.useraccount.repository.AccountBalanceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCancelService {

    private final AutoOrderService autoOrderService;
    private final AutoCancelRepository autoCancelRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry; // PQ 처리
    private final AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountBalanceCommand accountBalanceCommand;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    public void registerAutoCancel(AutoCancelRequestEvent request) {

        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(request.autoOrderId());
        var autoOrderEntity = result.autoOrderEntity();

        switch (result.previousStatus()) {
            case CANCELED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.ALREADY_CANCELLED)
            );
            case TRIGGERED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity,false, AutoCancelErrorCode.ALREADY_TRIGGERED)
            );
            default -> {

                autoOrderQueueRegistry.autoOrderCancel(request.autoOrderId(), request.stockCode());

                AutoCancelEntity autoCancelEntity = AutoCancelEntity.of(request.autoOrderId());
                autoCancelRepository.save(autoCancelEntity);

                if (autoOrderEntity.getAutoOrderType() == AutoOrderType.BUY) {
                    boolean refunded = accountBalanceCommand.refundReservedCash(autoOrderEntity.getUsername(), (long) autoOrderEntity.getOrderPrice() * autoOrderEntity.getOrderQuantity());

                    if (!refunded) {
                        log.error("Redis refund failed. orderId={}, username={}", autoOrderEntity.getAutoOrderId(), autoOrderEntity.getUsername());
                        autoCancelResponseEventPublisher.publish(
                                AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.INTERNAL_ERROR)
                        );
                        return;
                    }
                } else {
                    boolean refunded = accountBalanceCommand.refundReservedStock(autoOrderEntity.getUsername(), autoOrderEntity.getStockCode(),
                            autoOrderEntity.getOrderQuantity());

                    if (!refunded) {
                        log.error("Redis refund failed. orderId={}, username={}", autoOrderEntity.getAutoOrderId(), autoOrderEntity.getUsername());
                        autoCancelResponseEventPublisher.publish(
                                AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.INTERNAL_ERROR)
                        );
                        return;
                    }
                }

                var autoCancelResponseEvent = AutoCancelResponseEvent.of(autoOrderEntity, true, null);

                stockServerAutoOrderResponseRepository.delete(autoCancelResponseEvent.username(), autoCancelResponseEvent.autoOrderId());
                autoCancelResponseEventPublisher.publish(autoCancelResponseEvent);
                accountUpdateEventPublisher.publish(autoCancelResponseEvent.username());
            }
        }
    }
}
