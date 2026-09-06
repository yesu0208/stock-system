package arile.toy.stocksystem.stockserver.otococancel.service;

import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.otococancel.dto.OtocoCancelErrorCode;
import arile.toy.stocksystem.stockserver.otococancel.entity.OtocoCancelEntity;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.publisher.OtocoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otococancel.repository.OtocoCancelRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoCancelService {

    private final OtocoRepository otocoRepository;
    private final OtocoCancelRepository otocoCancelRepository;
    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final OtocoCancelResponseEventPublisher otocoCancelResponseEventPublisher;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final AccountApiClient accountApiClient;
    private final CancelService cancelService;

    @Transactional
    public void registerCancel(OtocoCancelRequestEvent request) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(request.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        switch (entity.getOtocoStatus()) {

            case CANCELED -> otocoCancelResponseEventPublisher.publish(
                    OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.ALREADY_CANCELLED));

            case COMPLETED -> otocoCancelResponseEventPublisher.publish(
                    OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.ALREADY_COMPLETED));

            case WAITING_ENTRY -> {
                try {
                    cancelWaitingEntry(entity);
                    publishSuccess(entity);
                } catch (Exception e) {
                    log.error("Otoco cancel(WAITING_ENTRY) failed. otocoId={}", entity.getOtocoId(), e);
                    otocoCancelResponseEventPublisher.publish(
                            OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.INTERNAL_ERROR));
                }
            }

            case ENTRY_ORDER_PLACED -> {
                cancelService.forceCancel(entity.getEntryOrderId());
                publishSuccess(entity);
            }

            case WAITING_EXIT -> {
                try {
                    cancelWaitingExit(entity);
                    publishSuccess(entity);
                } catch (Exception e) {
                    log.error("Otoco cancel(WAITING_EXIT) failed. otocoId={}", entity.getOtocoId(), e);
                    otocoCancelResponseEventPublisher.publish(
                            OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.INTERNAL_ERROR));
                }
            }
        }
    }

    @Transactional
    public void forceCancelOtoco(Long otocoId) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(otocoId)
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        try {
            switch (entity.getOtocoStatus()) {
                case WAITING_ENTRY -> {
                    cancelWaitingEntry(entity);
                    publishSuccess(entity);
                }
                case ENTRY_ORDER_PLACED -> cancelService.forceCancel(entity.getEntryOrderId());
                case WAITING_EXIT -> {
                    cancelWaitingExit(entity);
                    publishSuccess(entity);
                }
                default -> { /* 이미 종료 상태 - 아무 것도 하지 않음 */ }
            }
        } catch (Exception e) {
            log.error("Force otoco cancel failed. otocoId={}", otocoId, e);
        }
    }

    private void cancelWaitingEntry(OtocoEntity entity) {

        LeverageRatio leverageRatio = entity.getLeverageRatio();
        long orderAmount = (long) entity.getEntryTriggerPrice() * entity.getOrderQuantity();
        long refundAmount = leverageRatio.isSpot() ? orderAmount : leverageRatio.calculateMarginDeposit(orderAmount);

        boolean refunded = accountApiClient.refundReservedCash(entity.getUsername(), refundAmount);
        if (!refunded) {
            log.error("Otoco cash refund failed. otocoId={}, username={}", entity.getOtocoId(), entity.getUsername());
            throw new IllegalStateException("Cash refund failed");
        }

        otocoEntryBookRegistry.remove(entity.getStockCode(), entity.getOtocoId());

        entity.changeStatus(arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        otocoCancelRepository.save(OtocoCancelEntity.of(entity.getOtocoId()));
    }

    private void cancelWaitingExit(OtocoEntity entity) {

        LeverageRatio leverageRatio = entity.getLeverageRatio();

        boolean refunded = leverageRatio.isSpot()
                ? accountApiClient.refundReservedStock(entity.getUsername(), entity.getStockCode(), entity.getOrderQuantity())
                : accountApiClient.refundReservedLeverageStock(entity.getUsername(), entity.getStockCode(),
                leverageRatio.name(), entity.getOrderQuantity());

        if (!refunded) {
            log.error("Otoco stock refund failed. otocoId={}, username={}", entity.getOtocoId(), entity.getUsername());
            throw new IllegalStateException("Stock refund failed");
        }

        otocoExitBookRegistry.remove(entity.getStockCode(), entity.getOtocoId());

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        otocoCancelRepository.save(OtocoCancelEntity.of(entity.getOtocoId()));
    }

    private void publishSuccess(OtocoEntity entity) {
        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        otocoCancelResponseEventPublisher.publish(OtocoCancelResponseEvent.of(entity, true, null));
    }
}
