package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
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
public class OtocoQueueWarmupRunner {

    private final OtocoRepository otocoRepository;
    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final ExternalStockProperties externalStockProperties;

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<String> myStockCodes = externalStockProperties.getOpen();
        if (myStockCodes.isEmpty()) {
            return;
        }

        List<OtocoEntity> unfinished = otocoRepository.findAllUnfinished(myStockCodes);

        int entryCount = 0;
        int exitCount = 0;

        for (OtocoEntity entity : unfinished) {
            OtocoDto dto = OtocoDto.fromEntity(entity);

            if (entity.getOtocoStatus() == OtocoStatus.WAITING_ENTRY) {
                otocoEntryBookRegistry.register(dto);
                entryCount++;
            } else if (entity.getOtocoStatus() == OtocoStatus.WAITING_EXIT) {
                otocoExitBookRegistry.register(dto);
                exitCount++;
            }
        }

        log.info("Otoco queue warm-up complete. stockCodes={}, restoredEntry={}, restoredExit={}",
                myStockCodes, entryCount, exitCount);
    }
}
