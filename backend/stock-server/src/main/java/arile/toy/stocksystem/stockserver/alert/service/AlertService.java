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
import java.util.Optional;

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

    /** 소유자 검증 없이 취소 상태로 변경. 사용자 요청에는 updateAlertStatusByUserCancel을 사용할 것. */
    @Transactional
    public UpdateAlertStatusResult updateAlertStatusByCancel(Long alertId) {

        AlertEntity alertEntity = alertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() -> new IllegalArgumentException("alert not found"));

        return markCanceled(alertEntity);
    }

    /**
     * 사용자 취소 요청용.
     * 요청자가 알림 소유자이고 요청 종목코드가 알림 종목코드와 같을 때만 취소 상태로 변경.
     * 불일치 시 상태를 변경하지 않고 empty를 반환. (타인 알림 취소, 다른 샤드로의 취소 라우팅 방지)
     */
    @Transactional
    public Optional<UpdateAlertStatusResult> updateAlertStatusByUserCancel(
            Long alertId, String username, String stockCode) {

        AlertEntity alertEntity = alertRepository.findByIdForUpdate(alertId)
                .orElseThrow(() -> new IllegalArgumentException("alert not found"));

        if (username == null
                || !username.equals(alertEntity.getUsername())
                || !alertEntity.getStockCode().equals(stockCode)) {
            return Optional.empty();
        }

        return Optional.of(markCanceled(alertEntity));
    }

    private UpdateAlertStatusResult markCanceled(AlertEntity alertEntity) {

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
