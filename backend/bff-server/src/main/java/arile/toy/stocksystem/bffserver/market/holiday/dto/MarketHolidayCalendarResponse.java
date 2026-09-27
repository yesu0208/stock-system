package arile.toy.stocksystem.bffserver.market.holiday.dto;

import java.time.LocalDate;
import java.util.List;

public record MarketHolidayCalendarResponse(
        LocalDate today,
        LocalDate lastHolidayDate,
        List<MarketHolidayResponse> holidays
) {
    public static MarketHolidayCalendarResponse of(LocalDate today, List<MarketHolidayResponse> holidays) {
        LocalDate lastHolidayDate = holidays.isEmpty() ? null : holidays.getLast().holidayDate();
        return new MarketHolidayCalendarResponse(today, lastHolidayDate, holidays);
    }
}
