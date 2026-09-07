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

            fireAlert(alertDto, currentPrice);
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

            fireAlert(alertDto, currentPrice);
        }
    }

    private void fireAlert(AlertDto alertDto, int currentPrice) {

        UpdateAlertStatusResult result = alertService.updateAlertStatusByFire(alertDto.alertId());

        if (result.previousStatus() != AlertStatus.ACTIVE) {
            return;
        }

        stockServerAlertResponseRepository.delete(alertDto.username(), alertDto.alertId());
        alertResponseEventPublisher.publishFired(alertDto, currentPrice);

        log.info("Alert fired. alertId={}, username={}, stockCode={}, triggerPrice={}, currentPrice={}",
                alertDto.alertId(), alertDto.username(), alertDto.stockCode(), alertDto.triggerPrice(), currentPrice);
    }
}
