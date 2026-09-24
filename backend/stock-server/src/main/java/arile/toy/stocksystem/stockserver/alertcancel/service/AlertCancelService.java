package arile.toy.stocksystem.stockserver.alertcancel.service;

import arile.toy.stocksystem.stockserver.alert.dto.AlertQueueRegistry;
import arile.toy.stocksystem.stockserver.alert.dto.UpdateAlertStatusResult;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import arile.toy.stocksystem.stockserver.alert.service.AlertService;
import arile.toy.stocksystem.stockserver.alertcancel.dto.AlertCancelErrorCode;
import arile.toy.stocksystem.stockserver.alertcancel.entity.AlertCancelEntity;
import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelRequestEvent;
import arile.toy.stocksystem.stockserver.alertcancel.event.AlertCancelResponseEvent;
import arile.toy.stocksystem.stockserver.alertcancel.event.publisher.AlertCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alertcancel.repository.AlertCancelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertCancelService {

    private final AlertService alertService;
    private final AlertCancelRepository alertCancelRepository;
    private final AlertQueueRegistry alertQueueRegistry;
    private final AlertCancelResponseEventPublisher alertCancelResponseEventPublisher;
    private final StockServerAlertResponseRepository stockServerAlertResponseRepository;

    @Transactional
    public void registerAlertCancel(AlertCancelRequestEvent request) {

        // 요청자가 알림 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 알림 취소 방지)
        // 취소 응답은 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남김
        var ownedResult = alertService.updateAlertStatusByUserCancel(
                request.alertId(), request.username(), request.stockCode());

        if (ownedResult.isEmpty()) {
            log.warn("Alert cancel rejected: not the owner or stock code mismatch. alertId={}, requester={}, stockCode={}",
                    request.alertId(), request.username(), request.stockCode());
            return;
        }

        UpdateAlertStatusResult result = ownedResult.get();
        var alertEntity = result.alertEntity();

        switch (result.previousStatus()) {

            case CANCELED -> alertCancelResponseEventPublisher.publish(
                    AlertCancelResponseEvent.of(alertEntity, false, AlertCancelErrorCode.ALREADY_CANCELLED));

            case FIRED -> alertCancelResponseEventPublisher.publish(
                    AlertCancelResponseEvent.of(alertEntity, false, AlertCancelErrorCode.ALREADY_FIRED));

            default -> {

                try {
                    cancelInternal(alertEntity);
                    publishSuccess(alertEntity);

                } catch (Exception e) {

                    log.error("Alert cancel failed. alertId={}", alertEntity.getAlertId(), e);

                    alertCancelResponseEventPublisher.publish(
                            AlertCancelResponseEvent.of(alertEntity, false, AlertCancelErrorCode.INTERNAL_ERROR));
                    throw e;
                }
            }
        }
    }

    private void cancelInternal(AlertEntity alertEntity) {

        alertQueueRegistry.alertCancel(alertEntity.getAlertId(), alertEntity.getStockCode());

        alertCancelRepository.save(
                AlertCancelEntity.of(alertEntity.getAlertId())
        );
    }

    private void publishSuccess(AlertEntity alertEntity) {

        AlertCancelResponseEvent event = AlertCancelResponseEvent.of(alertEntity, true, null);

        stockServerAlertResponseRepository.delete(event.username(), event.alertId());

        alertCancelResponseEventPublisher.publish(event);
    }
}
