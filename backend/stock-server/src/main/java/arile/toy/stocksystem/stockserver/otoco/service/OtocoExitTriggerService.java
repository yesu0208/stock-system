package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.OtocoLockRegistry;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoLeg;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoExitTriggerService {

    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final OtocoLockRegistry otocoLockRegistry;
    private final OtocoExitTransactionalService otocoExitTransactionalService;

    public void getExternalTickMessageAndSettleExit(TradePriceTickMessage tick) {
        ReentrantLock lock = otocoLockRegistry.lock(tick.stockCode());
        lock.lock();
        try {
            settleWithinLock(tick);
        } finally {
            lock.unlock();
        }
    }

    private void settleWithinLock(TradePriceTickMessage tick) {

        String stockCode = tick.stockCode();
        int currentPrice = tick.curPrice();

        List<OtocoDto> snapshot = new ArrayList<>(otocoExitBookRegistry.getAll(stockCode));

        for (OtocoDto dto : snapshot) {

            boolean slHit = currentPrice <= dto.slTriggerPrice();
            boolean tpHit = currentPrice >= dto.tpTriggerPrice();

            if (!slHit && !tpHit) {
                continue;
            }

            OtocoLeg leg = slHit ? OtocoLeg.STOP_LOSS : OtocoLeg.TAKE_PROFIT;

            otocoExitBookRegistry.remove(stockCode, dto.otocoId());
            otocoExitTransactionalService.triggerExit(dto, leg);
        }
    }
}
