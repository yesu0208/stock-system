package arile.toy.stocksystem.bffserver.leverage.service;

import arile.toy.stocksystem.bffserver.exception.leverage.LeverageNotAllowedException;
import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.bffserver.rank.client.RankApiClient;
import arile.toy.stocksystem.bffserver.rank.dto.RankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeverageAccessValidator {

    private final RankApiClient rankApiClient;

    /**
     * 유저가 해당 레버리지 배율을 사용할 등급 자격이 있는지 검증한다.
     * 등급 제한이 없는 배율(SPOT, X1_5)은 즉시 통과.
     * 등급 API 조회 실패 시에는 안전하게 차단한다(fail-closed) - 단, SPOT/X1_5는 등급 조회 자체를 하지 않으므로
     * 등급 API 장애가 일반 현물 거래에는 영향을 주지 않는다.
     */
    public void validate(String username, LeverageRatio leverageRatio) {

        var requiredTier = leverageRatio.requiredTier();
        if (requiredTier == null) {
            return;
        }

        RankResponse rank = rankApiClient.getRank(username);

        if (rank == null) {
            log.warn("Rank lookup failed during leverage validation. username={}, leverageRatio={}", username, leverageRatio);
            throw new LeverageNotAllowedException();
        }

        if (!rank.isAtLeast(requiredTier)) {
            throw new LeverageNotAllowedException(requiredTier.name());
        }
    }
}
