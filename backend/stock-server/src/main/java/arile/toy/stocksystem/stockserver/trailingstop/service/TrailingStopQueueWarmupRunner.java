package arile.toy.stocksystem.stockserver.trailingstop.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopDto;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.registry.TrailingStopBookRegistry;
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
public class TrailingStopQueueWarmupRunner {

    private final TrailingStopService trailingStopService;
    private final TrailingStopBookRegistry trailingStopBookRegistry;
    private final ExternalStockProperties externalStockProperties;

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<String> myStockCodes = externalStockProperties.getOpen();
        if (myStockCodes.isEmpty()) {
            return;
        }

        List<TrailingStopEntity> activeTrailingStops =
                trailingStopService.findAllUntriggeredTrailingStops(myStockCodes);

        for (TrailingStopEntity entity : activeTrailingStops) {
            trailingStopBookRegistry.register(TrailingStopDto.fromEntity(entity));
        }

        log.info("Trailing stop queue warm-up complete. stockCodes={}, restoredTrailingStops={}",
                myStockCodes, activeTrailingStops.size());
    }
}
