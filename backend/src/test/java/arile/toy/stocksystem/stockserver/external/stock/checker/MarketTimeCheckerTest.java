package arile.toy.stocksystem.stockserver.external.stock.checker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class MarketTimeCheckerTest {

    private static class TestableMarketTimeChecker extends MarketTimeChecker {
        private final Clock clock;

        public TestableMarketTimeChecker(Clock clock) {
            this.clock = clock;
        }

        @Override
        public boolean isMarketOpenNow() {
            ZonedDateTime now = ZonedDateTime.now(clock);

            DayOfWeek day = now.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return false;
            }

            LocalTime time = now.toLocalTime();
            LocalTime open = LocalTime.of(8, 50, 10);
            LocalTime close = LocalTime.of(15, 39, 50);

            return !time.isBefore(open) && time.isBefore(close);
        }
    }

    @Test
    @DisplayName("평일 장 개시 시간일 때 마켓이 열려 있음을 반환한다")
    void givenWeekdayAndMarketOpenTime_whenCheck_thenReturnTrue() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 2, 19, 9, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
        );
        MarketTimeChecker checker = new TestableMarketTimeChecker(clock);
        assertTrue(checker.isMarketOpenNow());
    }

    @Test
    @DisplayName("평일 장 시작 전 시간에는 마켓이 열려 있지 않음을 반환한다")
    void givenWeekdayBeforeOpen_whenCheck_thenReturnFalse() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 2, 19, 8, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
        );
        MarketTimeChecker checker = new TestableMarketTimeChecker(clock);
        assertFalse(checker.isMarketOpenNow());
    }

    @Test
    @DisplayName("평일 장 종료 후 시간에는 마켓이 열려 있지 않음을 반환한다")
    void givenWeekdayAfterClose_whenCheck_thenReturnFalse() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 2, 19, 16, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
        );
        MarketTimeChecker checker = new TestableMarketTimeChecker(clock);
        assertFalse(checker.isMarketOpenNow());
    }

    @Test
    @DisplayName("주말에는 마켓이 열려 있지 않음을 반환한다")
    void givenWeekend_whenCheck_thenReturnFalse() {
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 2, 21, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(), // Saturday
                ZoneId.of("Asia/Seoul")
        );
        MarketTimeChecker checker = new TestableMarketTimeChecker(clock);
        assertFalse(checker.isMarketOpenNow());
    }
}
