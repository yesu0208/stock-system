package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeveragePositionRedisSyncerTest {

    private static final String USERNAME = "user1";
    private static final String STOCK_CODE = "005930";
    private static final String OTHER_KEY =
            LeverageAccountRedisRepository.positionKey("000660", LeverageRatio.X1_5);
    private static final LeveragePositionInfo OTHER_INFO =
            LeveragePositionInfo.of(3, 3, 300_000L, 300_050L, 100_000L, "NORMAL");

    @Mock
    private LeverageAccountRedisRepository leverageAccountRedisRepository;

    @InjectMocks
    private LeveragePositionRedisSyncer syncer;

    /** 다른 포지션 1개가 이미 들어 있는 Redis 포지션 맵 (syncer가 직접 수정하므로 가변 맵이어야 함) */
    private Map<String, LeveragePositionInfo> redisPositionsWith(String key, LeveragePositionInfo info) {
        Map<String, LeveragePositionInfo> positions = new HashMap<>();
        positions.put(OTHER_KEY, OTHER_INFO);
        if (key != null) {
            positions.put(key, info);
        }
        return positions;
    }

    @Test
    @DisplayName("sync: DB 포지션 값으로 Redis 포지션을 추가하고 다른 포지션은 보존한다")
    void sync_addsPosition() {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, STOCK_CODE, LeverageRatio.X2, 10, 700_000L, 700_100L);
        position.setAvailableQuantity(6);
        position.changeMarginStatus(MarginStatus.MARGIN_CALL, LocalDate.now());
        given(leverageAccountRedisRepository.getPositions(USERNAME)).willReturn(redisPositionsWith(null, null));

        syncer.sync(position);

        String key = LeverageAccountRedisRepository.positionKey(STOCK_CODE, LeverageRatio.X2);
        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of(
                OTHER_KEY, OTHER_INFO,
                key, LeveragePositionInfo.of(10, 6, 700_000L, 700_100L, 350_000L, "MARGIN_CALL")
        ));
    }

    @Test
    @DisplayName("sync: 이미 있는 포지션은 DB 값으로 덮어쓴다")
    void sync_overwritesExistingPosition() {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, STOCK_CODE, LeverageRatio.X2, 10, 700_000L, 700_100L);
        String key = LeverageAccountRedisRepository.positionKey(STOCK_CODE, LeverageRatio.X2);
        LeveragePositionInfo stale = LeveragePositionInfo.of(4, 4, 280_000L, 280_040L, 140_000L, "NORMAL");
        given(leverageAccountRedisRepository.getPositions(USERNAME)).willReturn(redisPositionsWith(key, stale));

        syncer.sync(position);

        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of(
                OTHER_KEY, OTHER_INFO,
                key, LeveragePositionInfo.of(10, 10, 700_000L, 700_100L, 350_000L, "NORMAL")
        ));
    }

    @Test
    @DisplayName("remove: 해당 종목·배율의 포지션만 제거하고 다른 포지션은 보존한다")
    void remove_removesOnlyTargetPosition() {
        String key = LeverageAccountRedisRepository.positionKey(STOCK_CODE, LeverageRatio.X2);
        LeveragePositionInfo target = LeveragePositionInfo.of(4, 4, 280_000L, 280_040L, 140_000L, "NORMAL");
        given(leverageAccountRedisRepository.getPositions(USERNAME)).willReturn(redisPositionsWith(key, target));

        syncer.remove(USERNAME, STOCK_CODE, LeverageRatio.X2);

        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of(OTHER_KEY, OTHER_INFO));
    }

    @Test
    @DisplayName("remove: 제거할 포지션이 없어도 나머지를 그대로 저장한다")
    void remove_whenAbsent_keepsOthers() {
        given(leverageAccountRedisRepository.getPositions(USERNAME)).willReturn(redisPositionsWith(null, null));

        syncer.remove(USERNAME, STOCK_CODE, LeverageRatio.X2);

        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of(OTHER_KEY, OTHER_INFO));
    }
}
