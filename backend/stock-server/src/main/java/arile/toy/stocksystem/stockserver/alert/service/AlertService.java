package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.*;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.publisher.AlertResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alert.repository.AlertRepository;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertQueueRegistry alertQueueRegistry;
    private final AlertResponseEventPublisher alertResponseEventPublisher;
    private final StockServerAlertResponseRepository stockServerAlertResponseRepository;

    public void registerAlert(AlertRequestEvent request) {

        AlertEntity savedAlert;

        try {
            AlertEntity alertEntity = AlertEntity.of(
                    request.username(),
                    request.stockCode(),
                    request.direction(),
                    request.triggerPrice(),
                    AlertStatus.ACTIVE
            );
            savedAlert = alertRepository.save(alertEntity);

            var alertDto = AlertDto.fromEntity(savedAlert);
            alertQueueRegistry.alertEnqueue(alertDto);

        } catch (Exception e) {
            alertResponseEventPublisher.publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
            throw e;
        }

        var responseMessage = StockServerAlertResponseMessage.of(savedAlert.getAlertId(),
                savedAlert.getUsername(), savedAlert.getStockCode(), savedAlert.getDirection(),
                savedAlert.getTriggerPrice(), savedAlert.getRegisteredTime());

        stockServerAlertResponseRepository.save(responseMessage);
        alertResponseEventPublisher.publishRegistered(responseMessage);
    }

    @Transactional
    public UpdateAlertStatusResult updateAlertStatusByCancel(Long alertId) {

        AlertEntity alertEntity = alertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() -> new IllegalArgumentException("alert not found"));

        AlertStatus prevStatus = alertEntity.getStatus();

        if (prevStatus == AlertStatus.CANCELED || prevStatus == AlertStatus.FIRED) {
            return UpdateAlertStatusResult.of(alertEntity, prevStatus);
        }

        alertEntity.changeStatus(AlertStatus.CANCELED);
        return UpdateAlertStatusResult.of(alertEntity, prevStatus);
    }

    @Transactional
    public UpdateAlertStatusResult updateAlertStatusByFire(Long alertId) {

        AlertEntity alertEntity = alertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() -> new IllegalArgumentException("alert not found"));

        AlertStatus prevStatus = alertEntity.getStatus();

        if (prevStatus != AlertStatus.ACTIVE) {
            return UpdateAlertStatusResult.of(alertEntity, prevStatus);
        }

        alertEntity.changeStatus(AlertStatus.FIRED);
        return UpdateAlertStatusResult.of(alertEntity, prevStatus);
    }

    @Transactional
    public List<AlertEntity> findAllActiveAlerts(List<String> stockCodes) {
        return alertRepository.findAllActive(stockCodes);
    }
}
