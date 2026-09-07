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

        UpdateAlertStatusResult result = alertService.updateAlertStatusByCancel(request.alertId());
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
