package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountMarginStatusSyncerTest {

    private static final String USERNAME = "user1";

    @Mock
    private LeveragePositionRepository leveragePositionRepository;

    @Mock
    private LeverageAccountRedisRepository leverageAccountRedisRepository;

    @InjectMocks
    private AccountMarginStatusSyncer syncer;

    private static List<LeveragePositionEntity> positionsWith(MarginStatus... statuses) {
        return Arrays.stream(statuses)
                .map(status -> {
                    LeveragePositionEntity position = LeveragePositionEntity.of(
                            USERNAME, "005930", LeverageRatio.X2, 10, 700_000L, 700_100L);
                    position.changeMarginStatus(status, status == MarginStatus.NORMAL ? null : LocalDate.now());
                    return position;
                })
                .toList();
    }

    static Stream<Arguments> worstStatusCases() {
        return Stream.of(
                Arguments.of("모두 NORMAL",
                        new MarginStatus[]{MarginStatus.NORMAL, MarginStatus.NORMAL}, "NORMAL"),
                Arguments.of("NORMAL + MARGIN_CALL",
                        new MarginStatus[]{MarginStatus.NORMAL, MarginStatus.MARGIN_CALL}, "MARGIN_CALL"),
                Arguments.of("MARGIN_CALL + LIQUIDATION_PENDING + NORMAL",
                        new MarginStatus[]{MarginStatus.MARGIN_CALL, MarginStatus.LIQUIDATION_PENDING,
                                MarginStatus.NORMAL}, "LIQUIDATION_PENDING")
        );
    }

    @ParameterizedTest(name = "{0} → {2}")
    @MethodSource("worstStatusCases")
    @DisplayName("resync: 보유 포지션 중 가장 심각한 마진 상태를 계좌 대표 상태로 저장한다")
    void resync_savesWorstStatus(String description, MarginStatus[] statuses, String expected) {
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(positionsWith(statuses));

        syncer.resync(USERNAME);

        verify(leverageAccountRedisRepository).saveMarginStatus(USERNAME, expected);
    }

    @Test
    @DisplayName("resync: 레버리지 포지션이 없으면 NORMAL로 저장한다")
    void resync_withoutPositions_savesNormal() {
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of());

        syncer.resync(USERNAME);

        verify(leverageAccountRedisRepository).saveMarginStatus(USERNAME, "NORMAL");
    }
}
