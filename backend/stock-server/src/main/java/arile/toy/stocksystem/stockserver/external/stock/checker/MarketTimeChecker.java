package arile.toy.stocksystem.stockserver.external.stock.checker;

import arile.toy.stocksystem.stockserver.market.holiday.repository.MarketHolidayRepository;
import arile.toy.stocksystem.stockserver.market.phase.StockServerMarketPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class MarketTimeChecker {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final LocalTime MORNING_CALL_START = LocalTime.of(8, 50, 5);
    private static final LocalTime OPEN_START          = LocalTime.of(9, 0, 0);
    private static final LocalTime CLOSING_CALL_START  = LocalTime.of(15, 20, 0);
    private static final LocalTime CLOSING_CALL_END    = LocalTime.of(15, 30, 0);
    private static final LocalTime AFTER_START         = LocalTime.of(16, 0, 5);
    private static final LocalTime AFTER_END           = LocalTime.of(20, 0, 0);

    private final MarketHolidayRepository marketHolidayRepository;

    public StockServerMarketPhase resolvePhase() {
        return resolvePhase(ZonedDateTime.now(KST));
    }

    public StockServerMarketPhase resolvePhase(ZonedDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return StockServerMarketPhase.CLOSED;
        }

        if (marketHolidayRepository.existsByHolidayDate(now.toLocalDate())) {
            return StockServerMarketPhase.CLOSED;
        }

        LocalTime time = now.toLocalTime();

        if (!time.isBefore(MORNING_CALL_START) && time.isBefore(OPEN_START)) {
            return StockServerMarketPhase.MORNING_CALL;
        }
        if (!time.isBefore(OPEN_START) && time.isBefore(CLOSING_CALL_START)) {
            return StockServerMarketPhase.OPEN;
        }
        if (!time.isBefore(CLOSING_CALL_START) && time.isBefore(CLOSING_CALL_END)) {
            return StockServerMarketPhase.CLOSING_CALL;
        }
        if (!time.isBefore(AFTER_START) && time.isBefore(AFTER_END)) {
            return StockServerMarketPhase.AFTER;
        }
        return StockServerMarketPhase.CLOSED;
    }

    public boolean isMarketOpenNow() {
        return resolvePhase().isOrderable();
    }

    public boolean shouldMaintainConnection() {
        return shouldMaintainConnection(ZonedDateTime.now(KST));
    }

    public boolean shouldMaintainConnection(ZonedDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        if (marketHolidayRepository.existsByHolidayDate(now.toLocalDate())) {
            return false;
        }

        if (resolvePhase(now).isOrderable()) {
            return true;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(CLOSING_CALL_END) && time.isBefore(AFTER_START);
    }

    public boolean isHoliday(LocalDate date) {
        return marketHolidayRepository.existsByHolidayDate(date);
    }

    public boolean isTodayHoliday() {
        return isHoliday(LocalDate.now(KST));
    }
}
