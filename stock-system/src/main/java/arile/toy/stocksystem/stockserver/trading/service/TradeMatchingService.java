package arile.toy.stocksystem.stockserver.trading.service;

import arile.toy.stocksystem.stockserver.external.stock.message.TradePriceTickMessage;
import arile.toy.stocksystem.stockserver.lock.StockLockRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeMatchingService {

    private final StockLockRegistry stockLockRegistry;

    public void getExternalTickMessageAndTrade(TradePriceTickMessage tradePriceTickMessage) {
        ReentrantLock lock = stockLockRegistry.lock(tradePriceTickMessage.stockCode());

        lock.lock();
        try {
            matchAndExecuteWithinLock(tradePriceTickMessage);
        } finally {
            lock.unlock();
        }
    }

    private void matchAndExecuteWithinLock(TradePriceTickMessage tradePriceTickMessage) {
    }
}

