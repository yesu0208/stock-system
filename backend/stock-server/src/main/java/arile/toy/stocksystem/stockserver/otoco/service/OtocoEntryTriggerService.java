package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.OtocoLockRegistry;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoEntryTriggerService {

    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoLockRegistry otocoLockRegistry;
    private final OtocoEntryTransactionalService otocoEntryTransactionalService;

    public void getExternalTickMessageAndTriggerEntry(TradePriceTickMessage tick) {
        ReentrantLock lock = otocoLockRegistry.lock(tick.stockCode());
        lock.lock();
        try {
            trackWithinLock(tick);
        } finally {
            lock.unlock();
        }
    }

    private void trackWithinLock(TradePriceTickMessage tick) {
        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        List<OtocoDto> snapshot = new ArrayList<>(otocoEntryBookRegistry.getAll(stockCode));

        for (OtocoDto dto : snapshot) {
            boolean isTriggered = dto.entryDirection() == OtocoEntryDirection.ABOVE
                    ? currentPrice >= dto.entryTriggerPrice()
                    : currentPrice <= dto.entryTriggerPrice();

            if (!isTriggered) {
                continue;
            }

            otocoEntryBookRegistry.remove(stockCode, dto.otocoId());

            try {
                otocoEntryTransactionalService.triggerEntryAndRegisterOrder(dto);
            } catch (Exception e) {
                // 트랜잭션 롤백으로 WAITING_ENTRY 유지: 북에 다시 등록해 다음 틱에 재시도
                // (같은 틱의 나머지 OTOCO 처리는 계속 진행)
                log.error("Otoco entry trigger failed. otocoId={}", dto.otocoId(), e);
                otocoEntryBookRegistry.register(dto);
            }
        }
    }
}
