package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.*;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.alert.event.AlertRequestEvent;
import arile.toy.stocksystem.stockserver.alert.event.publisher.AlertResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alert.repository.AlertRepository;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertQueueRegistry alertQueueRegistry;
    private final AlertResponseEventPublisher alertResponseEventPublisher;
    private final StockServerAlertResponseRepository stockServerAlertResponseRepository;

    public void registerAlert(AlertRequestEvent request) {

        AlertEntity savedAlert = null;

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
            // 저장 이후 실패 시 ACTIVE로 남겨 두면, 사용자는 실패 응답을 받았는데 재시작 워밍업으로
            // 알림이 복구되고(컨슈머 재시도 시 중복 등록까지) 나중에 발송될 수 있음 -> CANCELED로 무효화
            if (savedAlert != null) {
                try {
                    alertQueueRegistry.alertCancel(savedAlert.getAlertId(), savedAlert.getStockCode());
                    savedAlert.changeStatus(AlertStatus.CANCELED);
                    alertRepository.save(savedAlert);
                } catch (Exception cancelException) {
                    e.addSuppressed(cancelException);
                }
            }
            alertResponseEventPublisher.publishRegisterError(request, AlertErrorCode.INTERNAL_ERROR);
            throw e;
        }

        var responseMessage = StockServerAlertResponseMessage.of(savedAlert.getAlertId(),
                savedAlert.getUsername(), savedAlert.getStockCode(), savedAlert.getDirection(),
                savedAlert.getTriggerPrice(), savedAlert.getRegisteredTime());

        // 등록은 이미 완료됨: 응답 캐시 저장 실패가 예외로 전파되면 컨슈머 재시도로 중복 등록되므로 로그만 남김
        try {
            stockServerAlertResponseRepository.save(responseMessage);
        } catch (Exception e) {
            log.warn("Alert response save failed after registration. alertId={}", savedAlert.getAlertId(), e);
        }
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
