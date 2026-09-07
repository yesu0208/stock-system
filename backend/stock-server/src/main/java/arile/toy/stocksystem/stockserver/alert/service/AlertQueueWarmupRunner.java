package arile.toy.stocksystem.stockserver.alert.service;

import arile.toy.stocksystem.stockserver.alert.dto.AlertDto;
import arile.toy.stocksystem.stockserver.alert.dto.AlertQueueRegistry;
import arile.toy.stocksystem.stockserver.alert.entity.AlertEntity;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertQueueWarmupRunner {

    private final AlertService alertService;
    private final AlertQueueRegistry alertQueueRegistry;
    private final ExternalStockProperties externalStockProperties;

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<String> myStockCodes = externalStockProperties.getOpen();
        if (myStockCodes.isEmpty()) {
            return;
        }

        List<AlertEntity> activeAlerts =
                alertService.findAllActiveAlerts(myStockCodes);

        for (AlertEntity alert : activeAlerts) {
            alertQueueRegistry.alertEnqueue(AlertDto.fromEntity(alert));
        }

        log.info("Alert queue warm-up complete. stockCodes={}, restoredAlerts={}",
                myStockCodes, activeAlerts.size());
    }
}
