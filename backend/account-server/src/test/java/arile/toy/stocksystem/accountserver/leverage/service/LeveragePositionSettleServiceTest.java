package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeveragePositionInfo;
import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import arile.toy.stocksystem.accountserver.leverage.repository.LeverageAccountRedisRepository;
import arile.toy.stocksystem.accountserver.leverage.repository.LeveragePositionRepository;
import arile.toy.stocksystem.accountserver.useraccount.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeveragePositionSettleServiceTest {

    private static final String USERNAME = "user1";

    @Mock
    private LeveragePositionRepository leveragePositionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LeverageAccountRedisRepository leverageAccountRedisRepository;

    @Mock
    private AccountMarginStatusSyncer accountMarginStatusSyncer;

    @InjectMocks
    private LeveragePositionSettleService service;

    @Test
    @DisplayName("settleLeveragePositions: DB 포지션 전체로 Redis를 덮어쓴 뒤 계좌 마진 상태를 재계산한다")
    void settle_overwritesRedisWithDbPositions() {
        LeveragePositionEntity samsungX2 =
                LeveragePositionEntity.of(USERNAME, "005930", LeverageRatio.X2, 10, 700_000L, 700_100L);
        samsungX2.setAvailableQuantity(6);
        samsungX2.changeMarginStatus(MarginStatus.MARGIN_CALL, LocalDate.now());

        LeveragePositionEntity hynixX1_5 =
                LeveragePositionEntity.of(USERNAME, "000660", LeverageRatio.X1_5, 3, 300_000L, 300_045L);

        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of(samsungX2, hynixX1_5));

        service.settleLeveragePositions(Set.of(USERNAME));

        InOrder inOrder = inOrder(leverageAccountRedisRepository, accountMarginStatusSyncer);
        inOrder.verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of(
                LeverageAccountRedisRepository.positionKey("005930", LeverageRatio.X2),
                LeveragePositionInfo.of(10, 6, 700_000L, 700_100L, 350_000L, "MARGIN_CALL"),
                LeverageAccountRedisRepository.positionKey("000660", LeverageRatio.X1_5),
                LeveragePositionInfo.of(3, 3, 300_000L, 300_045L,
                        hynixX1_5.getLoanAmount(), "NORMAL")
        ));
        inOrder.verify(accountMarginStatusSyncer).resync(USERNAME);
    }

    @Test
    @DisplayName("settleLeveragePositions: DB에 포지션이 없으면 빈 맵으로 덮어써 Redis 잔여 포지션을 제거한다")
    void settle_withoutPositions_clearsRedis() {
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of());

        service.settleLeveragePositions(Set.of(USERNAME));

        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of());
        verify(accountMarginStatusSyncer).resync(USERNAME);
    }

    @Test
    @DisplayName("settleLeveragePositions: 여러 사용자를 각각 정산한다")
    void settle_multipleUsers() {
        given(leveragePositionRepository.findByUsername("user1")).willReturn(List.of());
        given(leveragePositionRepository.findByUsername("user2")).willReturn(List.of());

        service.settleLeveragePositions(Set.of("user1", "user2"));

        verify(leverageAccountRedisRepository).savePositions("user1", Map.of());
        verify(leverageAccountRedisRepository).savePositions("user2", Map.of());
        verify(accountMarginStatusSyncer).resync("user1");
        verify(accountMarginStatusSyncer).resync("user2");
    }

    @Test
    @DisplayName("settleAllLeveragePositions: 전체 username을 중복 없이 정산한다")
    void settleAll() {
        given(userAccountRepository.findAllUsernames()).willReturn(List.of(USERNAME, USERNAME));
        given(leveragePositionRepository.findByUsername(USERNAME)).willReturn(List.of());

        service.settleAllLeveragePositions();

        verify(leveragePositionRepository, times(1)).findByUsername(USERNAME);
        verify(leverageAccountRedisRepository).savePositions(USERNAME, Map.of());
    }
}
