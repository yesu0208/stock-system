package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;
import arile.toy.stocksystem.stockserver.alert.dto.AlertQueueRegistry;
import arile.toy.stocksystem.stockserver.alert.dto.AlertStatus;
import arile.toy.stocksystem.stockserver.alert.dto.UpdateAlertStatusResult;
import arile.toy.stocksystem.stockserver.alert.event.publisher.AlertResponseEventPublisher;
import arile.toy.stocksystem.stockserver.alert.repository.StockServerAlertResponseRepository;
import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.AlertLockRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerService {

    private final AlertQueueRegistry alertQueueRegistry;
    private final AlertLockRegistry alertLockRegistry;
    private final AlertService alertService;
    private final StockServerAlertResponseRepository stockServerAlertResponseRepository;
    private final AlertResponseEventPublisher alertResponseEventPublisher;

    public void getExternalTickMessageAndCheckAlerts(TradePriceTickMessage tradePriceTickMessage) {
        ReentrantLock lock = alertLockRegistry.lock(tradePriceTickMessage.stockCode());

        lock.lock();
        try {
            checkAlerts(tradePriceTickMessage);
        } finally {
            lock.unlock();
        }
    }

    private void checkAlerts(TradePriceTickMessage tick) {
        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        pollAndFireAbove(stockCode, currentPrice);
        pollAndFireBelow(stockCode, currentPrice);
    }

    private void pollAndFireAbove(String stockCode, int currentPrice) {
        while (true) {
            AlertDto alertDto = alertQueueRegistry.pollAbove(stockCode);

            if (alertDto == null) break;

            if (alertDto.triggerPrice() > currentPrice) {
                alertQueueRegistry.alertEnqueue(alertDto);
                break;
            }

            if (!fireAlert(alertDto, currentPrice)) {
                break;
            }
        }
    }

    private void pollAndFireBelow(String stockCode, int currentPrice) {
        while (true) {
            AlertDto alertDto = alertQueueRegistry.pollBelow(stockCode);

            if (alertDto == null) break;

            if (alertDto.triggerPrice() < currentPrice) {
                alertQueueRegistry.alertEnqueue(alertDto);
                break;
            }

            if (!fireAlert(alertDto, currentPrice)) {
                break;
            }
        }
    }

    /**
     * @return 다음 알림을 계속 처리해도 되면 true.
     *         상태 변경에 실패하면 알림을 큐에 되돌려 다음 틱에 재시도하고 false 반환
     *         (같은 알림을 곧바로 다시 꺼내는 무한 반복 방지)
     */
    private boolean fireAlert(AlertDto alertDto, int currentPrice) {

        UpdateAlertStatusResult result;
        try {
            result = alertService.updateAlertStatusByFire(alertDto.alertId());
        } catch (Exception e) {
            log.error("Alert fire status update failed. alertId={}", alertDto.alertId(), e);
            alertQueueRegistry.alertEnqueue(alertDto);
            return false;
        }

        if (result.previousStatus() != AlertStatus.ACTIVE) {
            return true;
        }

        // 발송 상태는 이미 확정됨: 응답 캐시 삭제 실패가 발송 알림을 막지 않도록 로그만 남김
        try {
            stockServerAlertResponseRepository.delete(alertDto.username(), alertDto.alertId());
        } catch (Exception e) {
            log.warn("Alert response delete failed after fire. alertId={}", alertDto.alertId(), e);
        }
        alertResponseEventPublisher.publishFired(alertDto, currentPrice);

        log.info("Alert fired. alertId={}, username={}, stockCode={}, triggerPrice={}, currentPrice={}",
                alertDto.alertId(), alertDto.username(), alertDto.stockCode(), alertDto.triggerPrice(), currentPrice);
        return true;
    }
}
