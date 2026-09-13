package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 장 마감 정산 시, DB(LeveragePositionEntity)를 기준으로 Redis
 * (account:leverage:{username})의 레버리지 포지션 전체를 다시 맞춤.
 * UserStockService.settleStocks()와 동일한 패턴이며, 기존에는 이 정산
 * 경로 자체가 누락되어 있어 Redis가 초기화되면 레버리지 포지션이
 * 영영 복원되지 않던 문제를 해결.
 *
 * savePositions()는 부분 병합이 아니라 전체 덮어쓰기이므로, 대상은
 * 반드시 "그 유저의 DB상 전체 포지션"이어야 함. (그룹별로 쪼개면
 * 다른 그룹 종목의 포지션이 삭제되는 사고가 발생함)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeveragePositionSettleService {

    private final LeveragePositionRepository leveragePositionRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeverageAccountRedisRepository leverageAccountRedisRepository;
    private final AccountMarginStatusSyncer accountMarginStatusSyncer;

    @Transactional
    public void settleLeveragePositions(Set<String> usernames) {

        for (String username : usernames) {

            List<LeveragePositionEntity> positions = leveragePositionRepository.findByUsername(username);

            Map<String, LeveragePositionInfo> positionsMap = new HashMap<>();
            for (LeveragePositionEntity entity : positions) {
                String key = LeverageAccountRedisRepository.positionKey(
                        entity.getStockCode(), entity.getLeverageRatio());

                positionsMap.put(key, LeveragePositionInfo.of(
                        entity.getQuantity(),
                        entity.getAvailableQuantity(),
                        entity.getPurchaseAmount(),
                        entity.getLoanAmount(),
                        entity.getMarginStatus().name()
                ));
            }

            leverageAccountRedisRepository.savePositions(username, positionsMap);

            // 포지션 구성이 바뀌었을 수 있으므로 계좌 대표 마진 상태도 함께 재계산
            accountMarginStatusSyncer.resync(username);

            log.info("Redis leverage positions updated for user {}. count={}", username, positionsMap.size());
        }

        log.info("Leverage position settlement completed for {} users.", usernames.size());
    }

    @Transactional
    public void settleAllLeveragePositions() {
        Set<String> allUsernames = new HashSet<>(userAccountRepository.findAllUsernames());
        settleLeveragePositions(allUsernames);
    }
}
