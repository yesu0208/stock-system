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

        // 취소 이력을 먼저 확정: 큐에서 먼저 뺀 뒤 저장이 실패해 롤백되면
        // ACTIVE인데 큐에는 없는 알림이 되어 재시작 전까지 발송되지 않음
        alertCancelRepository.saveAndFlush(
                AlertCancelEntity.of(alertEntity.getAlertId())
        );

        // 큐에 남더라도 발송 시 상태가 CANCELED라 무시되므로, 실패해도 롤백시키지 않음
        try {
            alertQueueRegistry.alertCancel(alertEntity.getAlertId(), alertEntity.getStockCode());
        } catch (Exception e) {
            log.warn("Alert queue remove failed after cancel. alertId={}", alertEntity.getAlertId(), e);
        }
    }

    private void publishSuccess(AlertEntity alertEntity) {

        AlertCancelResponseEvent event = AlertCancelResponseEvent.of(alertEntity, true, null);

        // 취소는 이미 완료됨: 응답 캐시 삭제 실패로 롤백되면 큐에서 빠진 ACTIVE 알림이 남으므로 로그만 남김
        try {
            stockServerAlertResponseRepository.delete(event.username(), event.alertId());
        } catch (Exception e) {
            log.warn("Alert response delete failed after cancel. alertId={}", event.alertId(), e);
        }

        alertCancelResponseEventPublisher.publish(event);
    }
}
