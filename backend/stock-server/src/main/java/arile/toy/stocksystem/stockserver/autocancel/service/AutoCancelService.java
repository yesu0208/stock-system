package arile.toy.stocksystem.stockserver.autocancel.service;

import arile.toy.stocksystem.stockserver.autocancel.dto.AutoCancelErrorCode;
import arile.toy.stocksystem.stockserver.autocancel.entity.AutoCancelEntity;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.AutoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.autocancel.event.publisher.AutoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.autocancel.repository.AutoCancelRepository;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderType;
import arile.toy.stocksystem.stockserver.autoorder.dto.UpdateAutoOrderStatusResult;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.repository.StockServerAutoOrderResponseRepository;
import arile.toy.stocksystem.stockserver.autoorder.sevice.AutoOrderService;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
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
    private final AccountApiClient accountApiClient;

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

        boolean refunded;

        if (autoOrderEntity.getAutoOrderType() == AutoOrderType.BUY) {

            refunded = accountApiClient.refundReservedCash(
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

            refunded = accountApiClient.refundReservedStock(
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

        autoCancelRepository.save(
                AutoCancelEntity.of(autoOrderEntity.getAutoOrderId())
        );

        autoOrderQueueRegistry.autoOrderCancel(
                autoOrderEntity.getAutoOrderId(),
                autoOrderEntity.getStockCode()
        );
    }

    private void publishSuccess(AutoOrderEntity autoOrderEntity) {

        AutoCancelResponseEvent event = AutoCancelResponseEvent.of(autoOrderEntity, true, null);

        stockServerAutoOrderResponseRepository.delete(event.username(), event.autoOrderId());

        autoCancelResponseEventPublisher.publish(event);
    }
}
