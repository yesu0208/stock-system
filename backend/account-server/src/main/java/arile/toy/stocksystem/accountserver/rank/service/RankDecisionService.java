package arile.toy.stocksystem.accountserver.rank.service;

import arile.toy.stocksystem.accountserver.rank.dto.RankLevel;
import arile.toy.stocksystem.accountserver.rank.entity.UserRankEntity;
import org.springframework.stereotype.Component;

@Component
public class RankDecisionService {

    public void applyDailyResult(UserRankEntity rank, double delta) {

        boolean belowGold5 = !rank.getHighestTierReached().isGold5OrAbove();

        long newRp;
        if (belowGold5) {
            newRp = delta < 0
                    ? rank.getRp()
                    : Math.round(rank.getRp() + delta);
        } else {
            newRp = Math.round(rank.getRp() + delta);
        }

        RankLevel theoreticalLevel = RankLevel.fromRp(newRp);

        RankLevel finalLevel;
        boolean safetyNetApplies =
                theoreticalLevel.ordinal() < RankLevel.GOLD_5.ordinal()
                        && rank.getHighestTierReached().isGold5OrAbove();

        if (safetyNetApplies) {
            finalLevel = RankLevel.GOLD_5;
            newRp = RankLevel.GOLD_5.getRpLower();
        } else {
            finalLevel = theoreticalLevel;
        }

        rank.setRp(newRp);
        rank.setCurrentLevel(finalLevel);

        if (theoreticalLevel.ordinal() > rank.getHighestTierReached().ordinal()) {
            rank.setHighestTierReached(theoreticalLevel);
        }
    }
}
