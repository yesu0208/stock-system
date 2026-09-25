package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRepository;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@DisplayName("[Checker] 장 시간 판정 테스트")
@ExtendWith(MockitoExtension.class)
class MarketTimeCheckerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 25);

    @InjectMocks private MarketTimeChecker sut;
    @Mock private MarketHolidayRepository marketHolidayRepository;

    @DisplayName("평일 시각별로 장 단계를 판정한다 (경계 포함)")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "08:50:04, CLOSED",
            "08:50:05, MORNING_CALL",
            "08:59:59, MORNING_CALL",
            "09:00:00, OPEN",
            "15:19:59, OPEN",
            "15:20:00, CLOSING_CALL",
            "15:29:59, CLOSING_CALL",
            "15:30:00, CLOSED",
            "16:00:04, CLOSED",
            "16:00:05, AFTER",
            "19:59:59, AFTER",
            "20:00:00, CLOSED"
    })
    void givenWeekdayTime_whenResolving_thenPhase(LocalTime time, StockServerMarketPhase expected) {
        given(marketHolidayRepository.existsByHolidayDate(FRIDAY)).willReturn(false);

        assertThat(sut.resolvePhase(ZonedDateTime.of(FRIDAY, time, KST))).isEqualTo(expected);
    }

    @DisplayName("주말은 휴장일 조회 없이 CLOSED, 평일 휴장일도 CLOSED")
    @Test
    void givenWeekendOrHoliday_whenResolving_thenClosed() {
        assertThat(sut.resolvePhase(ZonedDateTime.of(FRIDAY.plusDays(1), LocalTime.of(10, 0), KST)))
                .isEqualTo(StockServerMarketPhase.CLOSED);
        then(marketHolidayRepository).shouldHaveNoInteractions();

        given(marketHolidayRepository.existsByHolidayDate(FRIDAY)).willReturn(true);
        assertThat(sut.resolvePhase(ZonedDateTime.of(FRIDAY, LocalTime.of(10, 0), KST)))
                .isEqualTo(StockServerMarketPhase.CLOSED);
    }

    @DisplayName("휴장일 조회를 저장소에 위임한다")
    @Test
    void whenCheckingHoliday_thenDelegates() {
        given(marketHolidayRepository.existsByHolidayDate(any())).willAnswer(i -> FRIDAY.equals(i.getArgument(0)));

        assertThat(sut.isHoliday(FRIDAY)).isTrue();
        assertThat(sut.isHoliday(FRIDAY.plusDays(3))).isFalse();
    }

    @DisplayName("일요일도 휴장일 조회 없이 CLOSED")
    @Test
    void givenSunday_whenResolving_thenClosed() {
        assertThat(sut.resolvePhase(ZonedDateTime.of(FRIDAY.plusDays(2), LocalTime.of(10, 0), KST)))
                .isEqualTo(StockServerMarketPhase.CLOSED);
        then(marketHolidayRepository).shouldHaveNoInteractions();
    }

    @DisplayName("연결 유지: 거래 가능한 시간과 마감 동시호가 종료~애프터 시작 사이(15:30~16:00:05)에는 유지한다")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "08:50:04, false",
            "08:50:05, true",
            "12:00:00, true",
            "15:29:59, true",
            "15:30:00, true",
            "16:00:04, true",
            "16:00:05, true",
            "19:59:59, true",
            "20:00:00, false"
    })
    void givenWeekdayTime_whenCheckingConnection_thenMaintainsDuringSession(LocalTime time, boolean expected) {
        given(marketHolidayRepository.existsByHolidayDate(FRIDAY)).willReturn(false);

        assertThat(sut.shouldMaintainConnection(ZonedDateTime.of(FRIDAY, time, KST))).isEqualTo(expected);
    }

    @DisplayName("연결 유지: 주말(토·일)은 휴장일 조회 없이, 평일 휴장일은 조회 후 유지하지 않는다")
    @Test
    void givenWeekendOrHoliday_whenCheckingConnection_thenFalse() {
        assertThat(sut.shouldMaintainConnection(ZonedDateTime.of(FRIDAY.plusDays(1), LocalTime.of(10, 0), KST))).isFalse();
        assertThat(sut.shouldMaintainConnection(ZonedDateTime.of(FRIDAY.plusDays(2), LocalTime.of(10, 0), KST))).isFalse();
        then(marketHolidayRepository).shouldHaveNoInteractions();

        given(marketHolidayRepository.existsByHolidayDate(FRIDAY)).willReturn(true);
        assertThat(sut.shouldMaintainConnection(ZonedDateTime.of(FRIDAY, LocalTime.of(10, 0), KST))).isFalse();
    }

    @DisplayName("현재 시각 기준 메서드: 매일 휴장일이면 장이 닫혀 있고 연결을 유지하지 않으며, 오늘 휴장 여부는 한국 날짜로 조회한다")
    @Test
    void givenAlwaysHoliday_whenCheckingNow_thenClosed() {
        lenient().when(marketHolidayRepository.existsByHolidayDate(any())).thenReturn(true);

        assertThat(sut.resolvePhase()).isEqualTo(StockServerMarketPhase.CLOSED);
        assertThat(sut.isMarketOpenNow()).isFalse();
        assertThat(sut.shouldMaintainConnection()).isFalse();
        assertThat(sut.isTodayHoliday()).isTrue();
        then(marketHolidayRepository).should(atLeastOnce()).existsByHolidayDate(LocalDate.now(KST));
    }
}
