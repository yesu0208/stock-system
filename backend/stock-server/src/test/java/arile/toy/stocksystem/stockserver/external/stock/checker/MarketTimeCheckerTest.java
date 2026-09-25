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
}
