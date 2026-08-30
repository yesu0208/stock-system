package arile.toy.stocksystem.stockserver.autoorder.sevice;

import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderDto;
import arile.toy.stocksystem.stockserver.autoorder.dto.AutoOrderQueueRegistry;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoOrderQueueWarmupRunner {

    private final AutoOrderService autoOrderService;
    private final AutoOrderQueueRegistry autoOrderQueueRegistry;
    private final ExternalStockProperties externalStockProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<String> myStockCodes = externalStockProperties.getOpen();
        if (myStockCodes.isEmpty()) {
            return;
        }

        List<AutoOrderEntity> activeAutoOrders =
                autoOrderService.findAllUntriggeredAutoOrders(myStockCodes);

        for (AutoOrderEntity autoOrder : activeAutoOrders) {
            autoOrderQueueRegistry.autoOrderEnqueue(AutoOrderDto.fromEntity(autoOrder));
        }

        log.info("Auto order queue warm-up complete. stockCodes={}, restoredAutoOrders={}",
                myStockCodes, activeAutoOrders.size());
    }
}
