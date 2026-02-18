package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.trading.dto.auto.cancel.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.AutoOrderType;
import arile.toy.stocksystem.stockserver.trading.dto.auto.order.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.trading.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.trading.entity.AutoOrderEntity;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCancelService {

    private final AutoOrderService autoOrderService;
    private final AutoCancelRepository autoCancelRepository;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final AutoCancelResponseEventPublisher autoCancelResponseEventPublisher;
    private final StockServerAutoOrderResponseRepository stockServerAutoOrderResponseRepository;
    private final AccountBalanceCommand accountBalanceCommand;
    private final AccountUpdateEventPublisher accountUpdateEventPublisher;

    @Transactional
    public void registerAutoCancel(AutoCancelRequestEvent request) {

        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(request.autoOrderId());
        var autoOrderEntity = result.autoOrderEntity();

        switch (result.previousStatus()) {
            case CANCELED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.ALREADY_CANCELLED));

            case TRIGGERED -> autoCancelResponseEventPublisher.publish(
                    AutoCancelResponseEvent.of(autoOrderEntity,false, AutoCancelErrorCode.ALREADY_TRIGGERED));

            default -> {

                try {
                    cancelInternal(autoOrderEntity);
                    publishSuccess(autoOrderEntity);

                } catch (Exception e) {

                    log.error("Auto cancel failed. autoOrderId={}",
                            autoOrderEntity.getAutoOrderId(), e);

                    autoCancelResponseEventPublisher.publish(
                            AutoCancelResponseEvent.of(autoOrderEntity, false, AutoCancelErrorCode.INTERNAL_ERROR));
                }
            }
        }
    }

    @Transactional
    public void forceAutoCancel(Long autoOrderId) {

        UpdateAutoOrderStatusResult result = autoOrderService.updateAutoOrderStatusByCancel(autoOrderId);

        if (!result.previousStatus().isOpen()) {
            return;
        }

        var autoOrderEntity = result.autoOrderEntity();

        try {
            cancelInternal(autoOrderEntity);
            publishSuccess(autoOrderEntity);
        } catch (Exception e) {
            log.error("Force auto cancel failed. autoOrderId={}", autoOrderId, e);
        }
    }

    private void cancelInternal(AutoOrderEntity autoOrderEntity) {

        autoOrderQueueRegistry.autoOrderCancel(
                autoOrderEntity.getAutoOrderId(),
                autoOrderEntity.getStockCode()
        );

        autoCancelRepository.save(
                AutoCancelEntity.of(autoOrderEntity.getAutoOrderId())
        );

        boolean refunded;

        if (autoOrderEntity.getAutoOrderType() == AutoOrderType.BUY) {

            refunded = accountBalanceCommand.refundReservedCash(
                    autoOrderEntity.getUsername(),
                    (long) autoOrderEntity.getOrderPrice()
                            * autoOrderEntity.getOrderQuantity()
            );

            if (!refunded) {
                log.error("Redis cash refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Cash refund failed");
            }

        } else {

            refunded = accountBalanceCommand.refundReservedStock(
                    autoOrderEntity.getUsername(),
                    autoOrderEntity.getStockCode(),
                    autoOrderEntity.getOrderQuantity()
            );

            if (!refunded) {
                log.error("Redis stock refund failed. autoOrderId={}, username={}",
                        autoOrderEntity.getAutoOrderId(),
                        autoOrderEntity.getUsername());

                throw new IllegalStateException("Stock refund failed");
            }
        }
    }

    private void publishSuccess(AutoOrderEntity autoOrderEntity) {

        AutoCancelResponseEvent event = AutoCancelResponseEvent.of(autoOrderEntity, true, null);

        stockServerAutoOrderResponseRepository.delete(event.username(), event.autoOrderId());

        autoCancelResponseEventPublisher.publish(event);
        accountUpdateEventPublisher.publish(event.username());
    }
}
