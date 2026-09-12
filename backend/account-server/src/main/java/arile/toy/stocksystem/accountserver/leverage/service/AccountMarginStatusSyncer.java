package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 유저가 보유한 모든 레버리지 포지션 중 가장 심각한 마진 상태를,
 * 계좌 전체를 대표하는 단일 상태로 재계산해 Redis에 동기화
 */
@Component
@RequiredArgsConstructor
public class AccountMarginStatusSyncer {

    private final LeveragePositionRepository leveragePositionRepository;
    private final LeverageAccountRedisRepository leverageAccountRedisRepository;

    public void resync(String username) {
        List<LeveragePositionEntity> positions = leveragePositionRepository.findByUsername(username);

        String worst = positions.stream()
                .map(LeveragePositionEntity::getMarginStatus)
                .max(Comparator.comparingInt(this::severity))
                .map(Enum::name)
                .orElse(MarginStatus.NORMAL.name());

        leverageAccountRedisRepository.saveMarginStatus(username, worst);
    }

    private int severity(MarginStatus status) {
        return switch (status) {
            case NORMAL -> 0;
            case MARGIN_CALL -> 1;
            case LIQUIDATION_PENDING -> 2;
        };
    }
}