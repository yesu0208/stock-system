package arile.toy.stocksystem.accountserver.leverage.service;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.entity.LeveragePositionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LeverageInterestServiceTest {

    private static final String USERNAME = "user1";

    @Mock
    private LeverageInterestChargeExecutor leverageInterestChargeExecutor;

    private LeverageInterestService service;

    @BeforeEach
    void setUp() {
        service = new LeverageInterestService(new LeverageInterestCalculator(), leverageInterestChargeExecutor);
    }

    /** 대출금 365,000 → 하루 이자 85원 */
    private static LeveragePositionEntity position(long id, String stockCode, long loanAmount, LocalDate lastCharged) {
        LeveragePositionEntity position =
                LeveragePositionEntity.of(USERNAME, stockCode, LeverageRatio.X2, 10, 730_000L, 730_100L);
        position.setLoanAmount(loanAmount);
        position.setLastInterestChargedDate(lastCharged);
        ReflectionTestUtils.setField(position, "leveragePositionId", id);
        return position;
    }

    @Test
    @DisplayName("마지막 청구일부터 오늘까지의 경과 달력일수만큼 이자를 몰아서 청구하고 오늘 날짜를 청구 기준일로 넘긴다")
    void chargesElapsedDays() {
        LocalDate today = LocalDate.now();
        LeveragePositionEntity position = position(1L, "005930", 365_000L, today.minusDays(3));

        int charged = service.chargeInterestForAllPositions(List.of(position));

        assertThat(charged).isEqualTo(1);
        verify(leverageInterestChargeExecutor)
                .chargeInterestForOnePosition(USERNAME, "005930", 1L, 255L, today);
    }

    @ParameterizedTest(name = "대출금 {0}")
    @ValueSource(longs = {0L, -1L})
    @DisplayName("대출금이 없으면 청구하지 않는다")
    void whenNoLoan_skips(long loanAmount) {
        LeveragePositionEntity position = position(1L, "005930", loanAmount, LocalDate.now().minusDays(3));

        assertThat(service.chargeInterestForAllPositions(List.of(position))).isZero();

        verifyNoInteractions(leverageInterestChargeExecutor);
    }

    @ParameterizedTest(name = "마지막 청구일 = 오늘 + {0}일")
    @ValueSource(longs = {0L, 1L})
    @DisplayName("오늘 이미 청구했거나 청구일이 미래면 청구하지 않는다")
    void whenNoElapsedDays_skips(long plusDays) {
        LeveragePositionEntity position =
                position(1L, "005930", 365_000L, LocalDate.now().plusDays(plusDays));

        assertThat(service.chargeInterestForAllPositions(List.of(position))).isZero();

        verifyNoInteractions(leverageInterestChargeExecutor);
    }

    @Test
    @DisplayName("계산된 이자가 0원이면 청구하지 않는다")
    void whenInterestZero_skips() {
        LeveragePositionEntity position = position(1L, "005930", 1_000L, LocalDate.now().minusDays(1));

        assertThat(service.chargeInterestForAllPositions(List.of(position))).isZero();

        verifyNoInteractions(leverageInterestChargeExecutor);
    }

    @Test
    @DisplayName("청구에 실패한 포지션은 집계에서 제외하고, 나머지 포지션은 계속 청구한다")
    void whenOneFails_continuesOthers() {
        LocalDate today = LocalDate.now();
        LeveragePositionEntity failing = position(1L, "000660", 365_000L, today.minusDays(1));
        LeveragePositionEntity success = position(2L, "005930", 365_000L, today.minusDays(1));
        willThrow(new IllegalStateException("Account not found"))
                .given(leverageInterestChargeExecutor)
                .chargeInterestForOnePosition(USERNAME, "000660", 1L, 85L, today);

        int charged = service.chargeInterestForAllPositions(List.of(failing, success));

        assertThat(charged).isEqualTo(1);
        verify(leverageInterestChargeExecutor)
                .chargeInterestForOnePosition(USERNAME, "005930", 2L, 85L, today);
    }
}
