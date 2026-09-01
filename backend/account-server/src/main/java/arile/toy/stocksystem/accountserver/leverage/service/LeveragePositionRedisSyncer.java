package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LeveragePositionRedisSyncer {

    private final LeverageAccountRedisRepository leverageAccountRedisRepository;

    public void sync(LeveragePositionEntity position) {
        String username = position.getUsername();
        Map<String, LeveragePositionInfo> positions = leverageAccountRedisRepository.getPositions(username);
        positions.put(
                LeverageAccountRedisRepository.positionKey(position.getStockCode(), position.getLeverageRatio()),
                LeveragePositionInfo.of(position.getQuantity(), position.getAvailableQuantity(),
                        position.getPurchaseAmount(), position.getLoanAmount())
        );
        leverageAccountRedisRepository.savePositions(username, positions);
    }

    public void remove(String username, String stockCode, LeverageRatio leverageRatio) {
        Map<String, LeveragePositionInfo> positions = leverageAccountRedisRepository.getPositions(username);
        positions.remove(LeverageAccountRedisRepository.positionKey(stockCode, leverageRatio));
        leverageAccountRedisRepository.savePositions(username, positions);
    }
}
